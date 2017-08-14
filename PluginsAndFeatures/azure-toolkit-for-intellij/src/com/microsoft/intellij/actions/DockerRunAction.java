/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
                    Paths.get(project.getBasePath(), Constant.DOCKER_CONTEXT_FOLDER), null);
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
