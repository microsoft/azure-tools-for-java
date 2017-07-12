package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.azuretools.ijidea.utility.AzureAnAction;
import com.microsoft.intellij.container.ConsoleLogger;
import com.microsoft.intellij.container.Constant;
import com.microsoft.intellij.container.DockerRuntime;
import com.microsoft.intellij.container.utils.DockerUtil;
import com.microsoft.intellij.container.utils.WarUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yanzh on 7/10/2017.
 */
public class DockerRunAction extends AzureAnAction {
    @Override
    public void onActionPerformed(AnActionEvent anActionEvent) {
        Project project = DataKeys.PROJECT.getData(anActionEvent.getDataContext());
        JFrame frame = WindowManager.getInstance().getFrame(project);
        Path destinationPath;
        DockerClient docker;

        try {
            if (project == null) {
                throw new Exception(Constant.ERROR_NO_SELECTED_PROJECT);
            }
            destinationPath = Paths.get(project.getBasePath(),Constant.DOCKER_CONTEXT_FOLDER, project.getName() + ".war");
            // Initialize docker client according to env DOCKER_HOST &
            // DOCKER_CERT_PATH
            ConsoleLogger.info(Constant.MESSAGE_DOCKER_CONNECTING);
            DefaultDockerClient.Builder dockerBuilder = DockerRuntime.getInstance().getDockerBuilder();
            docker = dockerBuilder.build();
            // Stop running container
            String runningContainerId = DockerRuntime.getInstance().getRunningContainerId();
            if (DockerUtil.containerExists(docker, runningContainerId)) {
                int ret = JOptionPane.showConfirmDialog(frame, Constant.MESSAGE_CONFIRM_STOP_CONTAINER,
                        "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (ret == JOptionPane.OK_OPTION) {
                    DockerRuntime.getInstance().cleanRuningContainer();
                } else {
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ConsoleLogger.error(String.format(Constant.ERROR_RUNNING_DOCKER, e.getMessage()));
            sendTelemetryOnException(anActionEvent, e);
            return;
        }

        Observable.fromCallable(() -> {
            // export WAR file
            DefaultLoader.getIdeHelper().invokeAndWait(() -> {
                ConsoleLogger.info(String.format(Constant.MESSAGE_EXPORTING_PROJECT, destinationPath));
            });
            WarUtil.export(project, destinationPath);

            // build image based on WAR file
            DefaultLoader.getIdeHelper().invokeAndWait(() -> {
                ConsoleLogger.info(Constant.MESSAGE_BUILDING_IMAGE);
            });
            String imageName = DockerUtil.buildImage(docker, project,
                    Paths.get(project.getBasePath(), Constant.DOCKER_CONTEXT_FOLDER));
            DefaultLoader.getIdeHelper().invokeAndWait(() -> {
                ConsoleLogger.info(String.format(Constant.MESSAGE_IMAGE_INFO, imageName));
            });

            // create a container
            String containerId = DockerUtil.createContainer(docker, project, imageName);
            DefaultLoader.getIdeHelper().invokeAndWait(() -> {
                ConsoleLogger.info(Constant.MESSAGE_CREATING_CONTAINER);
                ConsoleLogger.info(String.format(Constant.MESSAGE_CONTAINER_INFO, containerId));
            });

            // start container
            DefaultLoader.getIdeHelper().invokeAndWait(() -> {
                ConsoleLogger.info(Constant.MESSAGE_STARTING_CONTAINER);
            });
            String webappUrl = DockerUtil.runContainer(docker, containerId);
            DefaultLoader.getIdeHelper().invokeAndWait(() -> {
                ConsoleLogger.info(String.format(Constant.MESSAGE_CONTAINER_STARTED, webappUrl, project.getName()));
            });
            return project.getName();
        }).subscribeOn(Schedulers.io()).subscribe(ret -> {
            Map<String, String> extraInfo = new HashMap<>();
            extraInfo.put("ProjectName", ret);
            sendTelemetryOnSuccess(anActionEvent, extraInfo);
        }, e -> {
            sendTelemetryOnException(anActionEvent, e);
        });
    }
}
