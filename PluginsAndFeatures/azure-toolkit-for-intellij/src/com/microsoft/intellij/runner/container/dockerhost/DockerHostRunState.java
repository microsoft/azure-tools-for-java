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
import com.microsoft.intellij.container.Constant;
import com.microsoft.intellij.container.utils.DockerUtil;
import com.microsoft.intellij.runner.RunProcessHandler;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import rx.Observable;

public class DockerHostRunState implements RunProfileState {
    private static final String DOCKER_CONTEXT_FOLDER_NAME = "dockerContext";
    private static final String DOCKER_FILE_NAME = "Dockerfile";
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
                    // locate war file to specified location
                    processHandler.setText("Locate war file ...  ");
                    String targetFilePath = dataModel.getTargetPath();
                    String targetBuildPath = Paths.get(targetFilePath).getParent().toString();
                    String targetFileName = dataModel.getTargetName();

                    FileUtils.copyFile(
                            Paths.get(targetBuildPath, targetFileName).toFile(),
                            Paths.get(targetBuildPath, DOCKER_CONTEXT_FOLDER_NAME, targetFileName).toFile()
                    );
                    // validate dockerfile
                    FileUtils.copyDirectory(
                            Paths.get(basePath, DOCKER_CONTEXT_FOLDER_NAME).toFile(),
                            Paths.get(targetBuildPath, DOCKER_CONTEXT_FOLDER_NAME).toFile()
                    );
                    // replace placeholder if exists
                    Path targetDockerfile = Paths.get(targetBuildPath, DOCKER_CONTEXT_FOLDER_NAME, DOCKER_FILE_NAME);
                    String content = new String(Files.readAllBytes(targetDockerfile));
                    content = content.replaceAll("<artifact>", targetFileName);
                    Files.write(targetDockerfile, content.getBytes());

                    // build image
                    processHandler.setText("Build image ...  ");
                    DockerClient docker = DockerUtil.getDockerClient(
                            dataModel.getDockerHost(),
                            dataModel.isTlsEnabled(),
                            dataModel.getDockerCertPath()
                    );
                    String latestImageName = DockerUtil.buildImage(docker,
                            String.format("%s:%s", dataModel.getImageName(), dataModel.getTagName()),
                            Paths.get(targetBuildPath, DOCKER_CONTEXT_FOLDER_NAME),
                            (message) -> {
                                if (message.error() != null) {
                                    throw new DockerException(message.error());
                                } else {
                                    processHandler.setText(message.stream());
                                }
                            }
                    );

                    // docker run
                    String containerId = DockerUtil.createContainer(
                            docker,
                            String.format("%s:%s", dataModel.getImageName(), dataModel.getTagName())
                    );
                    runningContainerId[0] = containerId;
                    Container container = DockerUtil.runContainer(docker, containerId);
                    // props
                    Map<String, String> props = new HashMap<>();

                    ImmutableList<Container.PortMapping> ports = container.ports();
                    if (ports != null) {
                        String port = null;
                        for (Container.PortMapping portMapping : ports) {
                            if (Constant.TOMCAT_SERVICE_PORT.equals(String.valueOf(portMapping.privatePort()))) {
                                port = String.valueOf(portMapping.publicPort());
                            }
                        }
                        if (port != null) {
                            props.put("publicPort", port);
                        }
                    }
                    String hostname = new URI(dataModel.getDockerHost()).getHost();
                    props.put("hostname", hostname != null ? hostname : "localhost");
                    return props;
                }
        ).subscribeOn(SchedulerProviderFactory.getInstance().getSchedulerProvider().io()).subscribe(
                (props) -> {
                    processHandler.setText("Container started ... ");
                    processHandler.setText(String.format(
                            Constant.MESSAGE_CONTAINER_STARTED,
                            String.format("%s:%s", props.get("hostname"), props.get("publicPort")),
                            FilenameUtils.removeExtension(dataModel.getTargetName())
                    ));
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
