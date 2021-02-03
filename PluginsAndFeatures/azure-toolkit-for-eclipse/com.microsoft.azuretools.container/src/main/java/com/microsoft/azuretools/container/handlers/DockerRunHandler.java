/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.container.handlers;

import java.nio.file.Paths;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;

import com.microsoft.azuretools.container.ConsoleLogger;
import com.microsoft.azuretools.container.Constant;
import com.microsoft.azuretools.container.DockerRuntime;
import com.microsoft.azuretools.container.ui.DockerRunDialog;
import com.microsoft.azuretools.container.utils.WarUtil;
import com.microsoft.azuretools.core.actions.MavenExecuteAction;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;
import com.microsoft.azuretools.core.utils.MavenUtils;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;

public class DockerRunHandler extends AzureAbstractHandler {

    private static final String MAVEN_GOALS = "package";
    private IProject project;
    private String basePath;
    private String destinationPath;

    @Override
    public Object onExecute(ExecutionEvent event) throws ExecutionException {
        project = PluginUtil.getSelectedProject();
        try {
            if (project == null) {
                throw new Exception(Constant.ERROR_NO_SELECTED_PROJECT);
            }
            basePath = project.getLocation().toString();
            if (MavenUtils.isMavenProject(project)) {
                destinationPath = MavenUtils.getTargetPath(project);
            } else {
                destinationPath = Paths.get(basePath, Constant.DOCKERFILE_FOLDER, project.getName() + ".war")
                        .normalize().toString();
            }

            // Stop running container
            String runningContainerId = DockerRuntime.getInstance().getRunningContainerId(basePath);
            if (runningContainerId != null) {
                boolean stop = MessageDialog.openConfirm(PluginUtil.getParentShell(), "Confirmation",
                        Constant.MESSAGE_CONFIRM_STOP_CONTAINER);
                if (stop) {
                    DockerRuntime.getInstance().cleanRuningContainer(basePath);
                } else {
                    return null;
                }
            }

            // Build artifact
            ConsoleLogger.info(String.format(Constant.MESSAGE_EXPORTING_PROJECT, destinationPath));
            if (MavenUtils.isMavenProject(project)) {
                MavenExecuteAction action = new MavenExecuteAction(MAVEN_GOALS);
                IContainer container;
                container = MavenUtils.getPomFile(project).getParent();
                action.launch(container, () -> {
                    // TODO: callback after mvn package done. IMPORTANT
                    buildAndRun(event);
                    return null;
                });
            } else {
                WarUtil.export(project, destinationPath);
                buildAndRun(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ConsoleLogger.error(String.format(Constant.ERROR_RUNNING_DOCKER, e.getMessage()));
            sendTelemetryOnException(event, e);
        }
        return null;
    }

    private void buildAndRun(ExecutionEvent event) {

        DefaultLoader.getIdeHelper().invokeAndWait(() -> {
            DockerRunDialog dialog = new DockerRunDialog(PluginUtil.getParentShell(), basePath, destinationPath);
            dialog.open();
        });
    }

}
