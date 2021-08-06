/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.localrun;

import com.google.gson.JsonObject;
import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.remote.RemoteConfiguration;
import com.intellij.execution.remote.RemoteConfigurationType;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.psi.PsiMethod;
import com.microsoft.azure.toolkit.intellij.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.BindingEnum;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionConfiguration;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.azuretools.utils.CommandUtils;
import com.microsoft.azuretools.utils.JsonUtils;
import com.microsoft.intellij.RunProcessHandler;
import com.microsoft.intellij.util.ReadStreamLineThread;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class FunctionRunState extends AzureRunProfileState<IFunctionApp> {

    private static final int DEFAULT_FUNC_PORT = 7071;
    private static final int DEFAULT_DEBUG_PORT = 5005;
    private static final int MAX_PORT = 65535;
    private static final String DEBUG_PARAMETERS =
            "\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=%s\"";
    private static final String HOST_JSON = "host.json";
    private static final String EXTENSION_BUNDLE = "extensionBundle";
    private static final String EXTENSION_BUNDLE_ID = "Microsoft.Azure.Functions.ExtensionBundle";
    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile("version \"(.*)\"");
    private static final ComparableVersion JAVA_9 = new ComparableVersion("9");
    private static final ComparableVersion FUNC_3 = new ComparableVersion("3");
    private static final ComparableVersion MINIMUM_JAVA_9_SUPPORTED_VERSION = new ComparableVersion("3.0.2630");
    private static final ComparableVersion MINIMUM_JAVA_9_SUPPORTED_VERSION_V2 = new ComparableVersion("2.7.2628");
    private static final BindingEnum[] FUNCTION_WITHOUT_FUNCTION_EXTENSION = { BindingEnum.HttpOutput,
                                                                               BindingEnum.HttpTrigger };
    private boolean isDebuggerLaunched;
    private File stagingFolder;
    private Process installProcess;
    private Process process;
    private Executor executor;
    private FunctionRunConfiguration functionRunConfiguration;

    public FunctionRunState(@NotNull Project project, FunctionRunConfiguration functionRunConfiguration, Executor executor) {
        super(project);
        this.executor = executor;
        this.functionRunConfiguration = functionRunConfiguration;
    }

    @AzureOperation(name = "function.launch_debugger", type = AzureOperation.Type.TASK)
    private void launchDebugger(final Project project, int debugPort) {
        final Runnable runnable = () -> {
            final RunManagerImpl manager = new RunManagerImpl(project);
            final ConfigurationFactory configFactory = RemoteConfigurationType.getInstance().getConfigurationFactories()[0];
            final RemoteConfiguration remoteConfig = new RemoteConfiguration(project, configFactory);
            remoteConfig.PORT = String.valueOf(debugPort);
            remoteConfig.HOST = "localhost";
            remoteConfig.USE_SOCKET_TRANSPORT = true;
            remoteConfig.SERVER_MODE = false;
            remoteConfig.setName("azure functions");

            final RunnerAndConfigurationSettings configuration = new RunnerAndConfigurationSettingsImpl(manager, remoteConfig, false);
            manager.setTemporaryConfiguration(configuration);
            ExecutionUtil.runConfiguration(configuration, ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.DEBUG));
        };
        AzureTaskManager.getInstance().runAndWait(runnable, AzureTask.Modality.ANY);
    }

    @Override
    @AzureOperation(name = "function.run.state", type = AzureOperation.Type.ACTION)
    protected IFunctionApp executeSteps(@NotNull RunProcessHandler processHandler, @NotNull Operation operation) throws Exception {
        // Prepare staging Folder
        validateFunctionRuntime(processHandler);
        stagingFolder = FunctionUtils.getTempStagingFolder();
        addProcessTerminatedListener(processHandler);
        prepareStagingFolder(stagingFolder, processHandler, operation);
        // Run Function Host
        runFunctionCli(processHandler, stagingFolder);
        return null;
    }

    @AzureOperation(
        name = "function.validate_runtime",
        params = {"this.functionRunConfiguration.getFuncPath()"},
        type = AzureOperation.Type.TASK
    )
    private void validateFunctionRuntime(RunProcessHandler processHandler) {
        try {
            final String funcPath = functionRunConfiguration.getFuncPath();
            if (StringUtils.isEmpty(funcPath)) {
                throw new AzureToolkitRuntimeException(message("function.run.error.runtimeNotFound"));
            }
            final ComparableVersion funcVersion = getFuncVersion();
            if (funcVersion == null) {
                throw new AzureToolkitRuntimeException(message("function.run.error.runtimeNotFound"));
            }
            final ComparableVersion javaVersion = getJavaVersion();
            if (javaVersion == null) {
                processHandler.setText(message("function.run.error.getJavaVersionFailed"));
                return;
            }
            if (javaVersion.compareTo(JAVA_9) < 0) {
                // No need validate function host version within java 8 or earlier
                return;
            }
            final ComparableVersion minimumVersion = funcVersion.compareTo(FUNC_3) >= 0 ? MINIMUM_JAVA_9_SUPPORTED_VERSION
                                                     : MINIMUM_JAVA_9_SUPPORTED_VERSION_V2;
            if (funcVersion.compareTo(minimumVersion) < 0) {
                throw new AzureToolkitRuntimeException(message("function.run.error.funcOutOfDate"));
            }
        } catch (IOException e) {
            throw new AzureToolkitRuntimeException(message("function.run.error.validateRuntimeFailed", e.getMessage()));
        }
    }

    @AzureOperation(
        name = "function.get_version",
        params = {"this.functionRunConfiguration.getFuncPath()"},
        type = AzureOperation.Type.TASK
    )
    private ComparableVersion getFuncVersion() throws IOException {
        final File func = new File(functionRunConfiguration.getFuncPath());
        final String funcVersion = CommandUtils.executeCommandAndGetOutput(func.getAbsolutePath(),
                                                                           new String[]{"-v"}, func.getParentFile());
        if (StringUtils.isEmpty(funcVersion)) {
            return null;
        }
        return new ComparableVersion(funcVersion);
    }

    // Get java runtime version following the strategy of function core tools
    // Get java version of JAVA_HOME first, fall back to use PATH if JAVA_HOME not exists
    @AzureOperation(
        name = "function.validate_jre",
        type = AzureOperation.Type.TASK
    )
    private ComparableVersion getJavaVersion() throws IOException {
        final String javaHome = System.getenv("JAVA_HOME");
        final File javaFile = StringUtils.isEmpty(javaHome) ? null : Paths.get(javaHome, "bin", "java").toFile();
        final File executeFolder = javaFile == null ? null : javaFile.getParentFile();
        final String command = javaFile == null ? "java" : javaFile.getAbsolutePath();
        final String javaVersion = CommandUtils.executeCommandAndGetOutput(command, new String[]{"-version"},
                                                                           executeFolder, true);
        if (StringUtils.isEmpty(javaVersion)) {
            return null;
        }
        final Matcher matcher = JAVA_VERSION_PATTERN.matcher(javaVersion);
        return matcher.find() ? new ComparableVersion(matcher.group(1)) : null;
    }

    @AzureOperation(
        name = "function|cli.run",
        params = {"stagingFolder.getName()"},
        type = AzureOperation.Type.SERVICE
    )
    private void runFunctionCli(RunProcessHandler processHandler, File stagingFolder)
            throws IOException, InterruptedException {
        isDebuggerLaunched = false;
        final int debugPort = findFreePortForApi(DEFAULT_DEBUG_PORT);
        final int funcPort = findFreePortForApi(Math.max(DEFAULT_FUNC_PORT, debugPort + 1));
        processHandler.println(message("function.run.hint.port", funcPort), ProcessOutputTypes.SYSTEM);
        process = getRunFunctionCliProcessBuilder(stagingFolder, funcPort, debugPort).start();
        // Redirect function cli output to console
        readInputStreamByLines(process.getInputStream(), inputLine -> {
            if (isDebugMode() && isFuncInitialized(inputLine) && !isDebuggerLaunched) {
                // launch debugger when func ready
                isDebuggerLaunched = true;
                launchDebugger(project, debugPort);
            }
            if (processHandler.isProcessRunning()) {
                processHandler.setText(inputLine);
            }
        });
        readInputStreamByLines(process.getErrorStream(), inputLine -> {
            if (processHandler.isProcessRunning()) {
                processHandler.println(inputLine, ProcessOutputTypes.STDERR);
            }
        });
        // Pending for function cli
        process.waitFor();
    }

    private boolean isFuncInitialized(String input) {
        return StringUtils.containsIgnoreCase(input, "Job host started") ||
            StringUtils.containsIgnoreCase(input, "Listening for transport dt_socket at address");
    }

    private void readInputStreamByLines(InputStream inputStream, Consumer<String> stringConsumer) {
        new ReadStreamLineThread(inputStream, stringConsumer).start();
    }

    private void addProcessTerminatedListener(RunProcessHandler processHandler) {
        processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                stopProcessIfAlive(process);
                stopProcessIfAlive(installProcess);
            }
        });
    }

    private ProcessBuilder getRunFunctionCliProcessBuilder(File stagingFolder, int funcPort, int debugPort) {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        final String funcPath = functionRunConfiguration.getFuncPath();
        String[] command = new String[]{funcPath, "host", "start", "--port", String.valueOf(funcPort)};
        if (isDebugMode()) {
            final String debugConfiguration = String.format(DEBUG_PARAMETERS, debugPort);
            command = ArrayUtils.addAll(command, new String[]{"--language-worker", "--", debugConfiguration});
        }
        processBuilder.command(command);
        processBuilder.directory(stagingFolder);
        return processBuilder;
    }

    private ProcessBuilder getRunFunctionCliExtensionInstallProcessBuilder(File stagingFolder) {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        final String funcPath = functionRunConfiguration.getFuncPath();
        String[] command = new String[]{funcPath, "extensions", "install", "--java"};
        processBuilder.command(command);
        processBuilder.directory(stagingFolder);
        return processBuilder;
    }

    @AzureOperation(
        name = "function.prepare_staging_folder_detail",
        params = {"stagingFolder.getName()", "this.functionRunConfiguration.getFuncPath()"},
        type = AzureOperation.Type.SERVICE
    )
    private void prepareStagingFolder(File stagingFolder,
                                      RunProcessHandler processHandler,
                                      final @NotNull Operation operation) throws Exception {
        AzureTaskManager.getInstance().read(() -> {
            final Path hostJsonPath = FunctionUtils.getDefaultHostJson(project);
            final Path localSettingsJson = Paths.get(functionRunConfiguration.getLocalSettingsJsonPath());
            final PsiMethod[] methods = FunctionUtils.findFunctionsByAnnotation(functionRunConfiguration.getModule());
            final Path folder = stagingFolder.toPath();
            try {
                Map<String, FunctionConfiguration> configMap =
                    FunctionUtils.prepareStagingFolder(folder, hostJsonPath, functionRunConfiguration.getModule(), methods);
                operation.trackProperty(TelemetryConstants.TRIGGER_TYPE, StringUtils.join(FunctionUtils.getFunctionBindingList(configMap), ","));
                final Map<String, String> appSettings = FunctionUtils.loadAppSettingsFromSecurityStorage(functionRunConfiguration.getAppSettingsKey());
                FunctionUtils.copyLocalSettingsToStagingFolder(folder, localSettingsJson, appSettings);

                final Set<BindingEnum> bindingClasses = getFunctionBindingEnums(configMap);
                if (isInstallingExtensionNeeded(bindingClasses, processHandler)) {
                    installProcess = getRunFunctionCliExtensionInstallProcessBuilder(stagingFolder).start();
                }
            } catch (AzureExecutionException | IOException e) {
                final String error = String.format("failed prepare staging folder[%s]", folder);
                throw new AzureToolkitRuntimeException(error, e);
            }
        });
        if (installProcess != null) {
            readInputStreamByLines(installProcess.getErrorStream(), inputLine -> {
                if (processHandler.isProcessRunning()) {
                    processHandler.println(inputLine, ProcessOutputTypes.STDERR);
                }
            });
            readInputStreamByLines(installProcess.getInputStream(), inputLine -> {
                if (processHandler.isProcessRunning()) {
                    processHandler.setText(inputLine);
                }
            });
            int exitCode = installProcess.waitFor();
            if (exitCode != 0) {
                throw new AzureExecutionException(message("function.run.error.installFuncFailed"));
            }
        }
    }

    private boolean isDebugMode() {
        return executor instanceof DefaultDebugExecutor;
    }

    private static int findFreePortForApi(int startPort) {
        ServerSocket socket = null;
        for (int port = startPort; port <= MAX_PORT; port++) {
            try {
                socket = new ServerSocket(port);
                return socket.getLocalPort();
            } catch (IOException e) {
                // swallow this exception
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // swallow this exception
                    }
                }
            }
        }
        return -1;
    }

    @Override
    protected Map<String, String> getTelemetryMap() {
        return functionRunConfiguration.getModel().getTelemetryProperties();
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.FUNCTION, TelemetryConstants.RUN_FUNCTION_APP);
    }

    @Override
    @AzureOperation(
        name = "function.complete_local_run",
        params = {"this.functionRunConfiguration.getFuncPath()"},
        type = AzureOperation.Type.TASK
    )
    protected void onSuccess(IFunctionApp result, RunProcessHandler processHandler) {
        stopProcessIfAlive(process);

        if (!processHandler.isProcessTerminated()) {
            processHandler.setText(message("function.run.hint.succeed"));
            processHandler.notifyComplete();
        }
        FunctionUtils.cleanUpStagingFolder(stagingFolder);
    }

    @Override
    protected void onFail(@NotNull Throwable error, @NotNull RunProcessHandler processHandler) {
        super.onFail(error, processHandler);
        stopProcessIfAlive(process);
        FunctionUtils.cleanUpStagingFolder(stagingFolder);
    }

    private boolean isInstallingExtensionNeeded(Set<BindingEnum> bindingTypes, RunProcessHandler processHandler) {
        final JsonObject hostJson = readHostJson(stagingFolder.getAbsolutePath());
        final JsonObject extensionBundle = hostJson == null ? null : hostJson.getAsJsonObject(EXTENSION_BUNDLE);
        if (extensionBundle != null && extensionBundle.has("id") &&
                StringUtils.equalsIgnoreCase(extensionBundle.get("id").getAsString(), EXTENSION_BUNDLE_ID)) {
            processHandler.println(message("function.run.hint.skipInstallExtensionBundle"), ProcessOutputTypes.STDOUT);
            return false;
        }
        final boolean isNonHttpTriggersExist = bindingTypes.stream().anyMatch(binding ->
                                                                                      !Arrays.asList(FUNCTION_WITHOUT_FUNCTION_EXTENSION).contains(binding));
        if (!isNonHttpTriggersExist) {
            processHandler.println(message("function.run.hint.skipInstallExtensionHttp"), ProcessOutputTypes.STDOUT);
            return false;
        }
        return true;
    }

    private static JsonObject readHostJson(String stagingFolder) {
        final File hostJson = new File(stagingFolder, HOST_JSON);
        return JsonUtils.readJsonFile(hostJson);
    }

    private static Set<BindingEnum> getFunctionBindingEnums(Map<String, FunctionConfiguration> configMap) {
        final Set<BindingEnum> result = new HashSet<>();
        configMap.values().forEach(configuration -> configuration.getBindings().
                forEach(binding -> result.add(binding.getBindingEnum())));
        return result;
    }

    private static void stopProcessIfAlive(final Process proc) {
        if (proc != null && proc.isAlive()) {
            proc.destroy();
        }
    }
}
