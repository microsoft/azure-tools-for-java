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
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.spotify.docker.client.DefaultDockerClient.Builder;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;

import java.nio.file.Paths;

public class DockerizeHandler extends AzureAbstractHandler {

    @Override
    public Object onExecute(ExecutionEvent event) throws ExecutionException {
        IProject project = PluginUtil.getSelectedProject();
        ConsoleLogger.info(Constant.MESSAGE_ADDING_DOCKER_SUPPORT);
        try {
            if (project == null) {
                throw new Exception(Constant.ERROR_NO_SELECTED_PROJECT);
            }
            String dockerFileContent = Constant.DOCKERFILE_CONTENT_TOMCAT;
            String artifactRelativePath = Constant.DOCKERFILE_ARTIFACT_PLACEHOLDER;
            DockerUtil.createDockerFile(project.getLocation().toString(), Constant.DOCKERFILE_FOLDER,
                    Constant.DOCKERFILE_NAME, String.format(dockerFileContent, artifactRelativePath));
            ConsoleLogger.info(String.format(Constant.MESSAGE_DOCKERFILE_CREATED,
                    Paths.get(Constant.DOCKERFILE_FOLDER, Constant.DOCKERFILE_NAME).toString()));
            Builder builder = DockerRuntime.getInstance().getDockerBuilder();
            ConsoleLogger.info(String.format(Constant.MESSAGE_DOCKER_HOST_INFO, builder.uri()));
            ConsoleLogger.info(Constant.MESSAGE_ADD_DOCKER_SUPPORT_OK);
            ConsoleLogger.info(Constant.MESSAGE_INSTRUCTION);
        } catch (Exception e) {
            e.printStackTrace();
            ConsoleLogger.error(String.format(Constant.ERROR_CREATING_DOCKERFILE, e.getMessage()));
        }
        return null;
    }
}
