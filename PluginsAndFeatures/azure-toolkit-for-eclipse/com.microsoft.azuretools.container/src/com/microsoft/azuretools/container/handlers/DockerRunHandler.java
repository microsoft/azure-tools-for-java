/**
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

package com.microsoft.azuretools.container.handlers;

import com.microsoft.azuretools.container.ConsoleLogger;
import com.microsoft.azuretools.container.Constant;
import com.microsoft.azuretools.container.DockerRuntime;
import com.microsoft.azuretools.container.utils.DockerUtil;
import com.microsoft.azuretools.container.utils.WarUtil;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.spotify.docker.client.DefaultDockerClient.Builder;
import com.spotify.docker.client.DockerClient;

import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class DockerRunHandler extends AzureAbstractHandler {

    @Override
    public Object onExecute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        IProject project = PluginUtil.getSelectedProject();
        String destinationPath;
        DockerClient docker;

        try {
            if (project == null) {
                throw new Exception(Constant.ERROR_NO_SELECTED_PROJECT);
            }
            destinationPath = project.getLocation() + Constant.DOCKER_CONTEXT_FOLDER + project.getName() + ".war";
            // Initialize docker client according to env DOCKER_HOST & DOCKER_CERT_PATH
            ConsoleLogger.info(Constant.MESSAGE_DOCKER_CONNECTING);
            Builder dockerBuilder = DockerRuntime.getInstance().getDockerBuilder();
            docker = dockerBuilder.build();
            // Stop running container
            String runningContainerId = DockerRuntime.getInstance().getRunningContainerId();
            if (DockerUtil.containerExists(docker, runningContainerId)) {
                boolean stop = MessageDialog.openConfirm(window.getShell(), "Confirmation",
                        Constant.MESSAGE_CONFIRM_STOP_CONTAINER);
                if (stop) {
                    DockerRuntime.getInstance().cleanRuningContainer();
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ConsoleLogger.error(String.format(Constant.ERROR_RUNNING_DOCKER, e.getMessage()));
            sendTelemetryOnException(event, e);
            return null;
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
                    project.getLocation() + Constant.DOCKER_CONTEXT_FOLDER);
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
            sendTelemetryOnSuccess(event, extraInfo);
        }, e -> {
            sendTelemetryOnException(event, e);
        });
        return null;
    }

}
