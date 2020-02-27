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
 *
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
import com.microsoft.intellij.runner.AzureRunProfileState;
import com.microsoft.intellij.runner.RunProcessHandler;
import com.microsoft.intellij.runner.functions.core.FunctionUtils;
import com.microsoft.intellij.util.ReadStreamLineThread;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Consumer;

public class FunctionRunState extends AzureRunProfileState<FunctionApp> {

    private static final int DEFAULT_FUNC_PORT = 7071;
    private static final int MAX_PORT = 65535;

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
        final File stagingFolder = new File(functionRunConfiguration.getStagingFolder());
        prepareStagingFolder(stagingFolder, processHandler);
        // Run Function Host
        runFunctionCli(processHandler, stagingFolder);
        return null;
    }

    private void runFunctionCli(RunProcessHandler processHandler, File stagingFolder) throws IOException, InterruptedException {
        final int funcPort = findFreePortForApi(DEFAULT_FUNC_PORT);
        final int debugPort = findFreePortForApi(funcPort + 1);
        processHandler.println(String.format("Func host start at port : %s", funcPort), ProcessOutputTypes.SYSTEM);
        process = getRunFunctionCliProcessBuilder(stagingFolder, funcPort, debugPort).start();
        // Add listener to close func.exe
        addProcessTerminatedListener(processHandler, process);
        // Redirect function cli output to console
        readInputStreamByLines(process.getInputStream(), inputLine -> {
            if (isDebugMode() && StringUtils.containsIgnoreCase(inputLine, "Job host started")) {
                // launch debugger when func ready
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
        final String funcPath = StringUtils.isEmpty(functionRunConfiguration.getFuncPath()) ? "func" : functionRunConfiguration.getFuncPath();
        final String debugConfiguration = String.format("\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=%s\"", debugPort);
        String[] command = {funcPath, "host", "start", "--port", String.valueOf(funcPort)};
        if (isDebugMode()) {
            command = ArrayUtils.addAll(command, new String[] {"--language-worker", "--", debugConfiguration});
        }
        processBuilder.command(command);
        processBuilder.directory(stagingFolder);
        return processBuilder;
    }

    private void prepareStagingFolder(File stagingFolder, RunProcessHandler processHandler) {
        ReadAction.run(() -> {
            final Path hostJsonPath = FunctionUtils.getDefaultHostJson(project);
            final Path localSettingsJson = Paths.get(functionRunConfiguration.getLocalSettingsJsonPath());
            final PsiMethod[] methods = FunctionUtils.findFunctionsByAnnotation(functionRunConfiguration.getModule());
            try {
                FunctionUtils.prepareStagingFolder(stagingFolder.toPath(), hostJsonPath, functionRunConfiguration.getModule(), methods);
                FunctionUtils.copyLocalSettingsToStagingFolder(stagingFolder.toPath(), localSettingsJson, functionRunConfiguration.getAppSettings());
            } catch (AzureExecutionException | IOException e) {
                processHandler.println(String.format("Failed to prepare staging folder, %s", e.getMessage()), ProcessOutputTypes.STDERR);
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
    protected void onSuccess(FunctionApp result, RunProcessHandler processHandler) {
        if (process.isAlive()) {
            process.destroy();
        }
        if (!processHandler.isProcessTerminated()) {
            processHandler.setText("Succeed!");
            processHandler.notifyComplete();
        }
    }

    @Override
    protected void onFail(String errMsg, RunProcessHandler processHandler) {
        if (process.isAlive()) {
            process.destroy();
        }
        if (!processHandler.isProcessTerminated()) {
            processHandler.setText("Fail!");
            processHandler.notifyComplete();
        }
    }
}
