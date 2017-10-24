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

package com.microsoft.intellij.runner.container.dockerhost;

import com.google.common.collect.ImmutableList;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.microsoft.azuretools.core.mvp.model.container.pojo.DockerHostRunSetting;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.runner.RunProcessHandler;
import com.microsoft.intellij.runner.container.utils.Constant;
import com.microsoft.intellij.runner.container.utils.DockerProgressHandler;
import com.microsoft.intellij.runner.container.utils.DockerUtil;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.Container;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import rx.Observable;

public class DockerHostRunState implements RunProfileState {
    private final DockerHostRunSetting dataModel;
    private final Project project;


    public DockerHostRunState(Project project, DockerHostRunSetting dataModel) {
        this.dataModel = dataModel;
        this.project = project;
    }

    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner programRunner) throws ExecutionException {
        final String[] runningContainerId = {null};
        final RunProcessHandler processHandler = new RunProcessHandler();
        ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(this.project).getConsole();
        processHandler.startNotify();
        consoleView.attachToProcess(processHandler);

        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void startNotified(ProcessEvent processEvent) {

            }

            @Override
            public void processTerminated(ProcessEvent processEvent) {
            }

            @Override
            public void processWillTerminate(ProcessEvent processEvent, boolean b) {
                try {
                    DockerClient docker = DockerUtil.getDockerClient(
                            dataModel.getDockerHost(),
                            dataModel.isTlsEnabled(),
                            dataModel.getDockerCertPath()
                    );
                    DockerUtil.stopContainer(docker, runningContainerId[0]);
                } catch (Exception e) {
                    // ignore
                }
                processHandler.notifyProcessTerminated(processEvent.getExitCode());
            }

            @Override
            public void onTextAvailable(ProcessEvent processEvent, Key key) {
            }
        });
        Observable.fromCallable(
                () -> {
                    processHandler.setText("Starting job ...  ");
                    String basePath = project.getBasePath();
                    if (basePath == null) {
                        processHandler.println("Project base path is null.", ProcessOutputTypes.STDERR);
                        throw new FileNotFoundException("Project base path is null.");
                    }
                    // locate artifact to specified location
                    String targetFilePath = dataModel.getTargetPath();
                    processHandler.setText(String.format("Locating artifact ... [%s]", targetFilePath));

                    // validate dockerfile
                    Path targetDockerfile = Paths.get(dataModel.getDockerFilePath());
                    processHandler.setText(String.format("Validating dockerfile ... [%s]", targetDockerfile));
                    if (!targetDockerfile.toFile().exists()) {
                        throw new FileNotFoundException("Dockerfile not found.");
                    }
                    // replace placeholder if exists
                    String content = new String(Files.readAllBytes(targetDockerfile));
                    content = content.replaceAll(Constant.DOCKERFILE_ARTIFACT_PLACEHOLDER,
                            Paths.get(basePath).toUri().relativize(Paths.get(targetFilePath).toUri()).getPath()
                    );
                    Files.write(targetDockerfile, content.getBytes());

                    // build image
                    String imageNameWithTag = String.format("%s:%s", dataModel.getImageName(), dataModel.getTagName());
                    processHandler.setText(String.format("Building image ...  [%s]", imageNameWithTag));
                    DockerClient docker = DockerUtil.getDockerClient(
                            dataModel.getDockerHost(),
                            dataModel.isTlsEnabled(),
                            dataModel.getDockerCertPath()
                    );

                    DockerUtil.buildImage(docker,
                            imageNameWithTag,
                            targetDockerfile.getParent(),
                            targetDockerfile.getFileName().toString(),
                            new DockerProgressHandler(processHandler)
                    );

                    // docker run
                    String containerId = DockerUtil.createContainer(
                            docker,
                            String.format("%s:%s", dataModel.getImageName(), dataModel.getTagName())
                    );
                    runningContainerId[0] = containerId;
                    Container container = DockerUtil.runContainer(docker, containerId);
                    // props
                    String hostname = new URI(dataModel.getDockerHost()).getHost();
                    String publicPort = null;
                    ImmutableList<Container.PortMapping> ports = container.ports();
                    if (ports != null) {
                        for (Container.PortMapping portMapping : ports) {
                            if (Constant.TOMCAT_SERVICE_PORT.equals(String.valueOf(portMapping.privatePort()))) {
                                publicPort = String.valueOf(portMapping.publicPort());
                            }
                        }
                    }
                    processHandler.setText(String.format(Constant.MESSAGE_CONTAINER_STARTED,
                            (hostname != null ? hostname : "localhost") + (publicPort != null ? ":" + publicPort : "")
                    ));
                    return null;
                }
        ).subscribeOn(SchedulerProviderFactory.getInstance().getSchedulerProvider().io()).subscribe(
                (props) -> {
                    processHandler.setText("Container started.");
                    sendTelemetry(true, null);
                },
                (err) -> {
                    err.printStackTrace();
                    processHandler.println(err.getMessage(), ProcessOutputTypes.STDERR);
                    processHandler.notifyProcessTerminated(0);
                    sendTelemetry(false, err.getMessage());
                }
        );
        return new DefaultExecutionResult(consoleView, processHandler);
    }

    // TODO: refactor later
    private void sendTelemetry(boolean success, @Nullable String errorMsg) {
        Map<String, String> map = new HashMap<>();
        map.put("Success", String.valueOf(success));
        String fileName = dataModel.getTargetName();
        if (null != fileName) {
            map.put("FileType", MavenRunTaskUtil.getFileType(fileName));
        } else {
            map.put("FileType", "");
        }
        if (!success) {
            map.put("ErrorMsg", errorMsg);
        }

        AppInsightsClient.createByType(AppInsightsClient.EventType.Action, "Docker", "Run", map);
    }
}
