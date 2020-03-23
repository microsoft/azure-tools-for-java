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

package com.microsoft.azuretools.container.handlers;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_DOCKER_FILE;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import java.nio.file.Paths;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.microsoft.azuretools.container.ConsoleLogger;
import com.microsoft.azuretools.container.Constant;
import com.microsoft.azuretools.container.utils.DockerUtil;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;
import com.microsoft.azuretools.core.utils.MavenUtils;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.spotify.docker.client.DefaultDockerClient;

public class DockerizeHandler extends AzureAbstractHandler {

    @Override
    public Object onExecute(ExecutionEvent event) throws ExecutionException {
        IProject project = PluginUtil.getSelectedProject();
        ConsoleLogger.info(Constant.MESSAGE_ADDING_DOCKER_SUPPORT);
        EventUtil.executeWithLog(WEBAPP, CREATE_DOCKER_FILE, (operation) -> {
            if (project == null) {
                throw new Exception(Constant.ERROR_NO_SELECTED_PROJECT);
            }
            String basePath = project.getLocation().toString();
            String dockerFileContent = Constant.DOCKERFILE_CONTENT_TOMCAT;
            String artifactRelativePath = Constant.DOCKERFILE_ARTIFACT_PLACEHOLDER;
            if (MavenUtils.isMavenProject(project)) {
                artifactRelativePath = Paths.get(basePath).toUri()
                        .relativize(Paths.get(MavenUtils.getTargetPath(project)).toUri()).toString();
                if ("war".equals(MavenUtils.getPackaging(project))) {
                    // maven war: tomcat
                    dockerFileContent = Constant.DOCKERFILE_CONTENT_TOMCAT;
                } else if ("jar".equals(MavenUtils.getPackaging(project))) {
                    // maven jar: spring
                    dockerFileContent = Constant.DOCKERFILE_CONTENT_SPRING;
                }
            } else {
                artifactRelativePath = Paths.get(Constant.DOCKERFILE_FOLDER, project.getName() + ".war").normalize()
                        .toString();
                dockerFileContent = Constant.DOCKERFILE_CONTENT_TOMCAT;
            }
            DockerUtil.createDockerFile(project.getLocation().toString(), Constant.DOCKERFILE_FOLDER,
                    Constant.DOCKERFILE_NAME, String.format(dockerFileContent, artifactRelativePath));
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
            openFile(project.getFile(Paths.get(Constant.DOCKERFILE_FOLDER, Constant.DOCKERFILE_NAME).toString()));
            ConsoleLogger.info(String.format(Constant.MESSAGE_DOCKERFILE_CREATED,
                    Paths.get(Constant.DOCKERFILE_FOLDER, Constant.DOCKERFILE_NAME).toString()));
            ConsoleLogger.info(Constant.MESSAGE_ADD_DOCKER_SUPPORT_OK);
            ConsoleLogger.info(String.format(Constant.MESSAGE_DOCKER_HOST_INFO, DefaultDockerClient.fromEnv().uri()));
        }, (e) -> {
                e.printStackTrace();
                ConsoleLogger.error(String.format(Constant.ERROR_CREATING_DOCKERFILE, e.getMessage()));
            });
        return null;
    }

    private void openFile(IFile file) {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        IMarker marker;
        try {
            marker = file.createMarker(IMarker.TEXT);
            IDE.openEditor(page, marker);
            marker.delete();

        } catch (CoreException e) {
            e.printStackTrace();
        }
    }
}
