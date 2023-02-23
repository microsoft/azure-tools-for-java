/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localrun;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.process.OSProcessUtil;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.remote.RemoteConfiguration;
import com.intellij.execution.remote.RemoteConfigurationType;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.psi.PsiMethod;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandlerMessenger;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.function.FunctionSupported;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.CommandUtils;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.BindingEnum;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionConfiguration;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.RunProcessHandler;
import com.microsoft.intellij.util.ReadStreamLineThread;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppActionsContributor.CONFIG_CORE_TOOLS;
import static com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppActionsContributor.DOWNLOAD_CORE_TOOLS;
import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

@Slf4j
public class FunctionRunState extends AzureRunProfileState<Boolean> {

    private static final int DEFAULT_FUNC_PORT = 7071;
    private static final int DEFAULT_DEBUG_PORT = 5005;
    private static final String DEBUG_PARAMETERS =
            "\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=%s\"";
    private static final String HOST_JSON = "host.json";
    private static final String EXTENSION_BUNDLE = "extensionBundle";
    private static final String EXTENSION_BUNDLE_ID = "Microsoft.Azure.Functions.ExtensionBundle";
    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile("version \"(.*)\"");
    private static final Pattern PORT_EXCEPTION_PATTERN = Pattern.compile("Port \\d+ is unavailable");
    private static final ComparableVersion JAVA_9 = new ComparableVersion("9");
    private static final ComparableVersion FUNC_3 = new ComparableVersion("3");
    private static final ComparableVersion MINIMUM_JAVA_9_SUPPORTED_VERSION = new ComparableVersion("3.0.2630");
    private static final ComparableVersion MINIMUM_JAVA_9_SUPPORTED_VERSION_V2 = new ComparableVersion("2.7.2628");
    private static final BindingEnum[] FUNCTION_WITHOUT_FUNCTION_EXTENSION = {BindingEnum.HttpOutput, BindingEnum.HttpTrigger};
    private boolean isDebuggerLaunched;
    private File stagingFolder;
    private Process installProcess;
    private Process process;
    private final Executor executor;
    private final FunctionRunConfiguration functionRunConfiguration;
    @Getter
    private List<Connection<?, ?>> connections = new ArrayList<>();

    public FunctionRunState(@NotNull Project project, FunctionRunConfiguration functionRunConfiguration, Executor executor) {
        super(project);
        this.executor = executor;
        this.functionRunConfiguration = functionRunConfiguration;
    }

    @AzureOperation(name = "boundary/function.launch_debugger")
    private void launchDebugger(final Project project, int debugPort) {
        final Runnable runnable = () -> {
            final RunManagerImpl manager = new RunManagerImpl(project);
            final RemoteConfiguration remoteConfig = (RemoteConfiguration) RemoteConfigurationType.getInstance().createTemplateConfiguration(project);
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
    @AzureOperation(name = "user/function.run_app")
    protected Boolean executeSteps(@NotNull RunProcessHandler processHandler, @NotNull Operation operation) throws Exception {
        // Prepare staging Folder
        OperationContext.current().setMessager(getProcessHandlerMessenger());
        validateFunctionRuntime();
        stagingFolder = FunctionUtils.getTempStagingFolder();
        addProcessTerminatedListener(processHandler);
        prepareStagingFolder(stagingFolder, processHandler, operation);
        // Run Function Host
        runFunctionCli(processHandler, stagingFolder);
        return true;
    }

    private void applyResourceConnection(Map<String, String> appSettings) {
        if (CollectionUtils.isEmpty(functionRunConfiguration.getConnections())) {
            return;
        }
        functionRunConfiguration.getConnections().stream()
                .filter(connection -> connection.getResource().getDefinition() instanceof FunctionSupported)
                .forEach(connection -> ((FunctionSupported) connection.getResource().getDefinition())
                        .getPropertiesForFunction(connection.getResource().getData(), connection)
                        .forEach((key, value) -> appSettings.put(key.toString(), value.toString())));
    }

    @AzureOperation(name = "internal/function.validate_runtime")
    private void validateFunctionRuntime() {
        final ComparableVersion funcVersion = getFuncVersion();
        final ComparableVersion javaVersion = getJavaVersion();
        if (funcVersion == null || javaVersion == null) {
            AzureMessager.getMessager().warning(message("function.skip_local_run_validation"));
            return;
        }
        if (javaVersion.compareTo(JAVA_9) < 0) {
            // No need validate function host version within java 8 or earlier
            return;
        }
        final ComparableVersion minimumVersion = funcVersion.compareTo(FUNC_3) >= 0 ? MINIMUM_JAVA_9_SUPPORTED_VERSION : MINIMUM_JAVA_9_SUPPORTED_VERSION_V2;
        if (funcVersion.compareTo(minimumVersion) < 0) {
            throw new AzureToolkitRuntimeException(message("function.run.error.funcOutOfDate"),
                    message("function.run.error.funcOutOfDate.tips"), DOWNLOAD_CORE_TOOLS, CONFIG_CORE_TOOLS);
        }
    }

    @Nullable
    @AzureOperation(name = "boundary/function.get_version.func", params = {"this.functionRunConfiguration.getFuncPath()"})
    private ComparableVersion getFuncVersion() {
        File funcFile = Optional.ofNullable(functionRunConfiguration.getFuncPath()).filter(StringUtils::isNotBlank).map(File::new).orElse(null);
        if (funcFile == null || !funcFile.exists()) {
            final File settingsFuncFile = Optional.ofNullable(Azure.az().config().getFunctionCoreToolsPath()).filter(StringUtils::isNotBlank).map(File::new).orElse(null);
            if (settingsFuncFile != null && settingsFuncFile.exists()) {
                funcFile = settingsFuncFile;
                functionRunConfiguration.setFuncPath(funcFile.getAbsolutePath());
                final AzureString message = AzureString.format("the configured Function Core Tools for this Run Configuration is not found, use the default one \"{0}\" instead.", funcFile.getAbsolutePath());
                AzureMessager.getDefaultMessager().warning(message);
            } else {
                throw new AzureToolkitRuntimeException(message("function.run.error.runtimeNotFound"), DOWNLOAD_CORE_TOOLS, CONFIG_CORE_TOOLS);
            }
        }
        try {
            final String funcVersion = CommandUtils.exec(String.format("%s -v", funcFile.getName()), funcFile.getParent());
            return StringUtils.isEmpty(funcVersion) ? null : new ComparableVersion(funcVersion);
        } catch (final IOException e) {
            // swallow exception to get func version
            log.info("Failed to get version of Azure Functions Core Tools", e);
            return null;
        }
    }

    // Get java runtime version following the strategy of function core tools
    // Get java version of JAVA_HOME first, fall back to use PATH if JAVA_HOME not exists
    @Nullable
    @AzureOperation(name = "boundary/function.validate_jre")
    private static ComparableVersion getJavaVersion() {
        try {
            final String javaHome = System.getenv("JAVA_HOME");
            final String executeFolder = FileUtil.exists(javaHome) ? Paths.get(javaHome, "bin").toString() : null;
            final String javaVersion = CommandUtils.exec("java -version", executeFolder, true);
            if (StringUtils.isEmpty(javaVersion)) {
                return null;
            }
            final Matcher matcher = JAVA_VERSION_PATTERN.matcher(javaVersion);
            return matcher.find() ? new ComparableVersion(matcher.group(1)) : null;
        } catch (Throwable e) {
            // swallow exception to get java version
            log.info("Failed to get java version", e);
            return null;
        }
    }

    @AzureOperation(name = "boundary/function.run_cli.folder", params = {"stagingFolder.getName()"})
    private int runFunctionCli(RunProcessHandler processHandler, File stagingFolder)
            throws IOException, InterruptedException {
        isDebuggerLaunched = false;
        final int debugPort = FunctionUtils.findFreePort(DEFAULT_DEBUG_PORT);
        process = getRunFunctionCliProcessBuilder(stagingFolder, debugPort).start();
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
        final String[] error = new String[1];
        readInputStreamByLines(process.getErrorStream(), inputLine -> {
            error[0] = inputLine;
            if (processHandler.isProcessRunning()) {
                processHandler.println(inputLine, ProcessOutputTypes.STDERR);
            }
        });
        // Pending for function cli
        int result = process.waitFor();
        if (result != 0) {
            throw new AzureToolkitRuntimeException(error[0]);
        }
        return result;
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

    private ProcessBuilder getRunFunctionCliProcessBuilder(File stagingFolder, int debugPort) {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        final String funcPath = functionRunConfiguration.getFuncPath();
        String[] command = new String[]{funcPath};
        command = ArrayUtils.addAll(command, functionRunConfiguration.getFunctionHostArguments().split(" "));
        if (isDebugMode()) {
            final String debugConfiguration = String.format(DEBUG_PARAMETERS, debugPort);
            command = ArrayUtils.addAll(command, "--language-worker", "--", debugConfiguration);
        }
        processBuilder.command(command);
        processBuilder.directory(stagingFolder);
        return processBuilder;
    }

    private ProcessBuilder getRunFunctionCliExtensionInstallProcessBuilder(File stagingFolder) {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        final String funcPath = functionRunConfiguration.getFuncPath();
        final String[] command = new String[]{funcPath, "extensions", "install", "--java"};
        processBuilder.command(command);
        processBuilder.directory(stagingFolder);
        return processBuilder;
    }

    @AzureOperation(name = "boundary/function.prepare_staging_folder.folder|app", params = {"stagingFolder.getName()", "this.functionRunConfiguration.getFuncPath()"})
    private void prepareStagingFolder(File stagingFolder,
                                      RunProcessHandler processHandler,
                                      final @NotNull Operation operation) throws Exception {
        final RunProcessHandlerMessenger messenger = new RunProcessHandlerMessenger(processHandler);
        OperationContext.current().setMessager(messenger);
        final Path hostJsonPath = Paths.get(functionRunConfiguration.getHostJsonPath());
        final Path localSettingsJson = Paths.get(functionRunConfiguration.getLocalSettingsJsonPath());
        final PsiMethod[] methods = ReadAction.compute(() -> FunctionUtils.findFunctionsByAnnotation(functionRunConfiguration.getModule()));
        final Path folder = stagingFolder.toPath();
        try {
            final Map<String, FunctionConfiguration> configMap =
                    FunctionUtils.prepareStagingFolder(folder, hostJsonPath, project, functionRunConfiguration.getModule(), methods);
            operation.trackProperty(TelemetryConstants.TRIGGER_TYPE, StringUtils.join(FunctionUtils.getFunctionBindingList(configMap), ","));
            final Map<String, String> appSettings = FunctionUtils.loadAppSettingsFromSecurityStorage(functionRunConfiguration.getAppSettingsKey());
            applyResourceConnection(appSettings);
            FunctionUtils.copyLocalSettingsToStagingFolder(folder, localSettingsJson, appSettings);

            final Set<BindingEnum> bindingClasses = getFunctionBindingEnums(configMap);
            if (isInstallingExtensionNeeded(bindingClasses, processHandler)) {
                installProcess = getRunFunctionCliExtensionInstallProcessBuilder(stagingFolder).start();
            }
        } catch (final AzureExecutionException | IOException e) {
            final String error = String.format("failed prepare staging folder[%s]", folder);
            throw new AzureToolkitRuntimeException(error, e);
        }
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
            final int exitCode = installProcess.waitFor();
            if (exitCode != 0) {
                throw new AzureExecutionException(message("function.run.error.installFuncFailed"));
            }
        }
    }

    private boolean isDebugMode() {
        return executor instanceof DefaultDebugExecutor;
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
    @AzureOperation(name = "boundary/function.complete_run.func", params = {"this.functionRunConfiguration.getFuncPath()"})
    protected void onSuccess(Boolean result, RunProcessHandler processHandler) {
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
        final Map<String, Object> hostJson = readHostJson(stagingFolder.getAbsolutePath());
        final Map<String, Object> extensionBundle = hostJson == null ? null : (Map<String, Object>) hostJson.get(EXTENSION_BUNDLE);
        if (extensionBundle != null && extensionBundle.containsKey("id") &&
                StringUtils.equalsIgnoreCase((CharSequence) extensionBundle.get("id"), EXTENSION_BUNDLE_ID)) {
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

    private static Map<String, Object> readHostJson(String stagingFolder) {
        final File hostJson = new File(stagingFolder, HOST_JSON);
        // noinspection unchecked
        return JsonUtils.readFromJsonFile(hostJson, Map.class);
    }

    private static Set<BindingEnum> getFunctionBindingEnums(Map<String, FunctionConfiguration> configMap) {
        final Set<BindingEnum> result = new HashSet<>();
        configMap.values().forEach(configuration -> configuration.getBindings().
                forEach(binding -> result.add(binding.getBindingEnum())));
        return result;
    }

    private static void stopProcessIfAlive(final Process proc) {
        if (proc != null && proc.isAlive()) {
            OSProcessUtil.killProcessTree(proc);
        }
    }
}
