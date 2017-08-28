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
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.runner.RunProcessHandler;
import com.microsoft.intellij.runner.container.utils.Constant;
import com.microsoft.intellij.runner.container.utils.DockerProgressHandler;
import com.microsoft.intellij.runner.container.utils.DockerUtil;
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
    private final DockerHostRunModel dataModel;
    private final Project project;


    public DockerHostRunState(Project project, DockerHostRunModel dockerHostRunModel) {
        this.dataModel = dockerHostRunModel;
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
                    Path targetDockerfile = Paths.get(basePath, Constant.DOCKERFILE_FOLDER, Constant.DOCKERFILE_NAME);
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

                    String latestImageName = DockerUtil.buildImage(docker,
                            imageNameWithTag,
                            Paths.get(basePath, Constant.DOCKERFILE_FOLDER),
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
        if (!success) {
            map.put("ErrorMsg", errorMsg);
        }

        AppInsightsClient.createByType(AppInsightsClient.EventType.Action, "Docker", "Run", map);
    }
}
