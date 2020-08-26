/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner.functions.localrun;

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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.psi.PsiMethod;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.runner.AzureRunProfileState;
import com.microsoft.intellij.runner.RunProcessHandler;
import com.microsoft.intellij.runner.functions.core.FunctionUtils;
import com.microsoft.intellij.util.CommandUtils;
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
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FunctionRunState extends AzureRunProfileState<FunctionApp> {

    private static final int DEFAULT_FUNC_PORT = 7071;
    private static final int DEFAULT_DEBUG_PORT = 5005;
    private static final int MAX_PORT = 65535;
    private static final String FAILED_TO_GET_JAVA_VERSION = "Failed to get java runtime version";
    private static final String FAILED_TO_VALIDATE_FUNCTION_RUNTIME = "Failed to validate function runtime, %s";
    private static final String DEBUG_PARAMETERS =
            "\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=%s\"";
    private static final String RUNTIME_NOT_FOUND = "Azure Functions Core Tools not found. " +
            "Please go to https://aka.ms/azfunc-install to install Azure Functions Core Tools. \n"
            + "If you have installed the core tools, please refer https://github.com/microsoft/azure-tools-for-java/wiki/FAQ to get the "
            + "core tools path and set the value in function run configuration.";
    private static final String FUNCTION_CORE_TOOLS_OUT_OF_DATE = "Local function core tools didn't support java 9 or higher runtime, " +
            "to update it, see: https://aka.ms/azfunc-install.";
    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile("version \"(.*)\"");
    private static final ComparableVersion JAVA_9 = new ComparableVersion("9");
    private static final ComparableVersion FUNC_3 = new ComparableVersion("3");
    private static final ComparableVersion MINIMUM_JAVA_9_SUPPORTED_VERSION = new ComparableVersion("3.0.2630");
    private static final ComparableVersion MINIMUM_JAVA_9_SUPPORTED_VERSION_V2 = new ComparableVersion("2.7.2628");

    private boolean isDebuggerLaunched;
    private File stagingFolder;
    private Process process;
    private Executor executor;
    private FunctionRunConfiguration functionRunConfiguration;

    public FunctionRunState(@NotNull Project project, FunctionRunConfiguration functionRunConfiguration, Executor executor) {
        super(project);
        this.executor = executor;
        this.functionRunConfiguration = functionRunConfiguration;
    }

    @Override
    protected String getDeployTarget() {
        return "null";
    }

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
        ApplicationManager.getApplication().invokeAndWait(runnable, ModalityState.any());
    }

    @Override
    protected FunctionApp executeSteps(@NotNull RunProcessHandler processHandler, @NotNull Map<String, String> telemetryMap) throws Exception {
        // Prepare staging Folder
        updateTelemetryMap(telemetryMap);
        validateFunctionRuntime(processHandler);
        stagingFolder = FunctionUtils.getTempStagingFolder();
        prepareStagingFolder(stagingFolder, processHandler);
        // Run Function Host
        runFunctionCli(processHandler, stagingFolder);
        return null;
    }

    private void validateFunctionRuntime(RunProcessHandler processHandler) throws AzureExecutionException {
        try {
            final String funcPath = functionRunConfiguration.getFuncPath();
            if (StringUtils.isEmpty(funcPath)) {
                throw new AzureExecutionException(RUNTIME_NOT_FOUND);
            }
            final ComparableVersion funcVersion = getFuncVersion();
            if (funcVersion == null) {
                throw new AzureExecutionException(RUNTIME_NOT_FOUND);
            }
            final ComparableVersion javaVersion = getJavaVersion();
            if (javaVersion == null) {
                processHandler.setText(FAILED_TO_GET_JAVA_VERSION);
                return;
            }
            if (javaVersion.compareTo(JAVA_9) < 0) {
                // No need validate function host version within java 8 or earlier
                return;
            }
            final ComparableVersion minimumVersion = funcVersion.compareTo(FUNC_3) >= 0
                                                     ? MINIMUM_JAVA_9_SUPPORTED_VERSION
                                                     : MINIMUM_JAVA_9_SUPPORTED_VERSION_V2;
            if (funcVersion.compareTo(minimumVersion) < 0) {
                throw new AzureExecutionException(FUNCTION_CORE_TOOLS_OUT_OF_DATE);
            }
        } catch (IOException e) {
            throw new AzureExecutionException(String.format(FAILED_TO_VALIDATE_FUNCTION_RUNTIME, e.getMessage()));
        }
    }

    private ComparableVersion getFuncVersion() throws IOException {
        final File func = new File(functionRunConfiguration.getFuncPath());
        final String funcVersion = CommandUtils.executeCommandAndGetOutput(func.getAbsolutePath(), new String[]{"-v"}, func.getParentFile());
        if (StringUtils.isEmpty(funcVersion)) {
            return null;
        }
        return new ComparableVersion(funcVersion);
    }

    // Get java runtime version following the strategy of function core tools
    // Get java version of JAVA_HOME first, fall back to use PATH if JAVA_HOME not exists
    private ComparableVersion getJavaVersion() throws IOException {
        final String javaHome = System.getenv("JAVA_HOME");
        final File javaFile = StringUtils.isEmpty(javaHome) ? null : Paths.get(javaHome, "bin", "java").toFile();
        final File executeFolder = javaFile == null ? null : javaFile.getParentFile();
        final String command = javaFile == null ? "java" : javaFile.getAbsolutePath();
        final String javaVersion = CommandUtils.executeCommandAndGetOutput(command, new String[]{"-version"}, executeFolder);
        if (StringUtils.isEmpty(javaVersion)) {
            return null;
        }
        final Matcher matcher = JAVA_VERSION_PATTERN.matcher(javaVersion);
        return matcher.find() ? new ComparableVersion(matcher.group(1)) : null;
    }

    private void runFunctionCli(RunProcessHandler processHandler, File stagingFolder)
            throws IOException, InterruptedException {
        isDebuggerLaunched = false;
        final int debugPort = findFreePortForApi(DEFAULT_DEBUG_PORT);
        final int funcPort = findFreePortForApi(Math.max(DEFAULT_FUNC_PORT, debugPort + 1));
        processHandler.println(String.format("Using port : %s", funcPort), ProcessOutputTypes.SYSTEM);
        process = getRunFunctionCliProcessBuilder(stagingFolder, funcPort, debugPort).start();
        // Add listener to close func.exe
        addProcessTerminatedListener(processHandler, process);
        // Redirect function cli output to console
        readInputStreamByLines(process.getInputStream(), inputLine -> {
            if (isDebugMode() && StringUtils.containsIgnoreCase(inputLine, "Job host started") && !isDebuggerLaunched) {
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

    private void readInputStreamByLines(InputStream inputStream, Consumer<String> stringConsumer) {
        new ReadStreamLineThread(inputStream, stringConsumer).run();
    }

    private void addProcessTerminatedListener(RunProcessHandler processHandler, Process process) {
        processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                if (process != null && process.isAlive()) {
                    process.destroy();
                }
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

    private void prepareStagingFolder(File stagingFolder, RunProcessHandler processHandler) throws AzureExecutionException {
        ReadAction.run(() -> {
            final Path hostJsonPath = FunctionUtils.getDefaultHostJson(project);
            final Path localSettingsJson = Paths.get(functionRunConfiguration.getLocalSettingsJsonPath());
            final PsiMethod[] methods = FunctionUtils.findFunctionsByAnnotation(functionRunConfiguration.getModule());
            try {
                FunctionUtils.prepareStagingFolder(stagingFolder.toPath(), hostJsonPath, functionRunConfiguration.getModule(), methods);
                FunctionUtils.copyLocalSettingsToStagingFolder(stagingFolder.toPath(), localSettingsJson, functionRunConfiguration.getAppSettings());
            } catch (AzureExecutionException | IOException e) {
                throw new AzureExecutionException("Failed to prepare staging folder", e);
            }
        });
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
    protected void updateTelemetryMap(@NotNull Map<String, String> telemetryMap) {
        telemetryMap.putAll(functionRunConfiguration.getModel().getTelemetryProperties(telemetryMap));
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.FUNCTION, TelemetryConstants.RUN_FUNCTION_APP);
    }

    @Override
    protected void onSuccess(FunctionApp result, RunProcessHandler processHandler) {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
        if (!processHandler.isProcessTerminated()) {
            processHandler.setText("Function execute succeed.");
            processHandler.notifyComplete();
        }
        FunctionUtils.cleanUpStagingFolder(stagingFolder);
    }

    @Override
    protected void onFail(String errMsg, RunProcessHandler processHandler) {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
        if (!processHandler.isProcessTerminated()) {
            processHandler.println(errMsg, ProcessOutputTypes.STDERR);
            processHandler.notifyComplete();
        }
        FunctionUtils.cleanUpStagingFolder(stagingFolder);
    }
}
