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
import com.microsoft.azuretools.ijidea.utility.AzureAnAction;
import com.microsoft.intellij.container.ConsoleLogger;
import com.microsoft.intellij.container.Constant;
import com.microsoft.intellij.container.DockerRuntime;
import com.microsoft.intellij.container.utils.DockerUtil;
import com.spotify.docker.client.DefaultDockerClient;

import java.nio.file.Paths;

public class DockerizeAction extends AzureAnAction {
    @Override
    public void onActionPerformed(AnActionEvent anActionEvent) {
        Project project = DataKeys.PROJECT.getData(anActionEvent.getDataContext());
        ConsoleLogger.info(Constant.MESSAGE_ADDING_DOCKER_SUPPORT);
        try {
            if (project == null) {
                throw new Exception(Constant.ERROR_NO_SELECTED_PROJECT);
            }
            DockerUtil.createDockerFile(project, Constant.DOCKER_CONTEXT_FOLDER, Constant.DOCKERFILE_NAME,
                    Constant.DOCKERFILE_CONTENT_TOMCAT);
            ConsoleLogger.info(String.format(Constant.MESSAGE_DOCKERFILE_CREATED, Paths.get(Constant.DOCKER_CONTEXT_FOLDER, Constant.DOCKERFILE_NAME)));
            DefaultDockerClient.Builder builder = DockerRuntime.getInstance().getDockerBuilder();
            ConsoleLogger.info(String.format(Constant.MESSAGE_DOCKER_HOST_INFO, builder.uri()));
            ConsoleLogger.info(Constant.MESSAGE_ADD_DOCKER_SUPPORT_OK);
            ConsoleLogger.info(Constant.MESSAGE_INSTRUCTION);
        } catch (Exception e) {
            e.printStackTrace();
            ConsoleLogger.error(String.format(Constant.ERROR_CREATING_DOCKERFILE, e.getMessage()));
        }
    }
}
