package com.microsoft.intellij.container.run.remote;

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
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.intellij.container.Constant;
import com.microsoft.intellij.container.utils.DockerUtil;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

import rx.Observable;
import rx.schedulers.Schedulers;

public class ContainerRemoteRunState implements RunProfileState {
    private final ContainerRemoteRunModel containerRemoteRunModel;
    private final Project project;

    private final RunProcessHandler processHandler = new RunProcessHandler();

    public ContainerRemoteRunState(Project project, ContainerRemoteRunModel containerRemoteRunModel) {
        this.containerRemoteRunModel = containerRemoteRunModel;
        this.project = project;
    }

    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner programRunner) throws ExecutionException {
        ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(this.project).getConsole();
        processHandler.startNotify();
        consoleView.attachToProcess(processHandler);
        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void startNotified(ProcessEvent processEvent) {}

            @Override
            public void processTerminated(ProcessEvent processEvent) {}

            @Override
            public void processWillTerminate(ProcessEvent processEvent, boolean b) {
                processHandler.notifyProcessTerminated(0);
            }

            @Override
            public void onTextAvailable(ProcessEvent processEvent, Key key) {}
        });

        Observable.fromCallable(
                () -> {
                    println("Starting job ...  ");
                    ContainerRemoteRunModel.WebAppOnLinuxInfo webInfo = containerRemoteRunModel.getWebAppOnLinuxInfo();

                    // locate war file to specified location
                    println("Locate war file ...  ");
                    List<MavenProject> mavenProjects = MavenProjectsManager.getInstance(project).getRootProjects();
                    String targetBuildPath = new File(mavenProjects.get(0).getBuildDirectory()).getPath()
                            + File.separator + mavenProjects.get(0).getFinalName() + ".war";
                    String fileName = mavenProjects.get(0).getFinalName() + ".war";

                    // build image
                    println("Build image ...  ");
                    String dockerContent = String.format(Constant.DOCKERFILE_CONTENT_TOMCAT, project.getName());
                    DockerUtil.createDockerFile(project, "target", "Dockerfile", dockerContent);
                    DockerClient docker = DefaultDockerClient.fromEnv().build();
                    String latestImageName = DockerUtil.buildImage(docker, project,
                            new File(mavenProjects.get(0).getBuildDirectory()).toPath());

                    // push to ACR
                    println("Push to ACR ...  ");
                    ProgressHandler progressHandler = message -> {
                        // TODO: progress output
                        if (message.error() != null) {
                            throw new DockerException(message.toString());
                        }
                    };
                    ContainerRemoteRunModel.AzureContainerRegistryInfo acrInfo = containerRemoteRunModel
                            .getAzureContainerRegistryInfo();
                    DockerUtil.pushImage(docker, acrInfo.getServerUrl(), acrInfo.getUsername(), acrInfo.getPassword(),
                            latestImageName, acrInfo.getImageNameWithTag(), progressHandler);

                    // update WebApp
                    println("Update WebApp ...  ");
                    WebApp app = null;
                    try {
                        app = AzureWebAppMvpModel.getInstance().getWebAppById(webInfo.getSubscriptionId(),
                                webInfo.getWebAppId());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (app != null) {
                        app.update().withPrivateRegistryImage(acrInfo.getImageNameWithTag(), acrInfo.getServerUrl())
                                .withCredentials(acrInfo.getUsername(), acrInfo.getPassword())
                                .withStartUpCommand(acrInfo.getStartupFile())
                                .apply();
                    }
                    if (app != null && app.name() != null) {
                        println(String.format("URL:  http://%s.azurewebsites.net/%s", app.name(), project.getName()));
                    }
                    return null;
                }
        ).subscribeOn(Schedulers.io()).subscribe(
                (res) -> {
                    println("Job done");
                    processHandler.notifyProcessTerminated(0);
                },
                (err) -> {
                    err.printStackTrace();
                    errorln(err.getMessage());
                    processHandler.notifyProcessTerminated(0);
                }
        );
        return new DefaultExecutionResult(consoleView, processHandler);
    }

    private void println(String message, Key type) {
        if (!processHandler.isProcessTerminating() && !processHandler.isProcessTerminated()) {
            processHandler.notifyTextAvailable(message + "\n", type);
        } else {
            throw new Error("The process has been terminated");
        }
    }

    private void println(String message) {
        println(message, ProcessOutputTypes.SYSTEM);
    }

    private void errorln(String message) {
        println(message, ProcessOutputTypes.STDERR);
    }
}
