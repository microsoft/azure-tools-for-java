package com.microsoft.intellij.runner.container.dockerhost;

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
import com.microsoft.intellij.container.Constant;
import com.microsoft.intellij.runner.RunProcessHandler;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.File;
import java.util.List;

import static com.microsoft.intellij.container.utils.DockerUtil.buildImage;
import static com.microsoft.intellij.container.utils.DockerUtil.createDockerFile;

public class DockerHostRunState implements RunProfileState {
    private final DockerHostRunModel dockerHostRunModel;
    private final Project project;

    final RunProcessHandler processHandler = new RunProcessHandler();

    public DockerHostRunState(Project project, DockerHostRunModel dockerHostRunModel) {
        this.dockerHostRunModel = dockerHostRunModel;
        this.project = project;
    }

    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner programRunner) throws ExecutionException {
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
                processHandler.notifyProcessTerminated(0);
            }

            @Override
            public void onTextAvailable(ProcessEvent processEvent, Key key) {
            }
        });

        Observable.fromCallable(
                () -> {
                    println("Starting job ...  ");
                    // TODO: build war file to specified location
                    println("Locating war file ...  ");
                    List<MavenProject> mavenProjects = MavenProjectsManager.getInstance(project).getRootProjects();
                    String targetBuildPath = new File(mavenProjects.get(0).getBuildDirectory()).getPath() + File.separator + mavenProjects.get(0).getFinalName() + ".war";
                    String fileName = mavenProjects.get(0).getFinalName() + ".war";
                    // TODO: build image
                    println("Build image ...  ");
                    String dockerContent = String.format(Constant.DOCKERFILE_CONTENT_TOMCAT, project.getName() + ".war");
                    createDockerFile(project, "target", "Dockerfile", dockerContent);
                    DockerClient docker = DefaultDockerClient.fromEnv().build();
                    String latestImageName = buildImage(docker, project, new File(mavenProjects.get(0).getBuildDirectory()).toPath(), null);
                    // TODO: docker run

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

    private void println(String message) {
        println(message, ProcessOutputTypes.SYSTEM);
    }

    private void errorln(String message) {
        println(message, ProcessOutputTypes.STDERR);
    }


    private void println(String message, Key type) {
        if (!processHandler.isProcessTerminating() && !processHandler.isProcessTerminated()) {
            processHandler.notifyTextAvailable(message + "\n", type);
        } else {
            throw new Error("The process has been terminated");
        }
    }
}
