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

package com.microsoft.intellij.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.ijidea.utility.AzureAnAction;
import com.microsoft.intellij.container.Constant;
import com.microsoft.intellij.container.DockerRuntime;
import com.microsoft.intellij.container.utils.DockerUtil;
import com.spotify.docker.client.DefaultDockerClient;

import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class AddDockerSupportAction extends AzureAnAction {
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";
    private static final String NOTIFICATION_TITLE = "Add Docker Support";
    private Project project;

    @Override
    public void onActionPerformed(AnActionEvent anActionEvent) {
        project = DataKeys.PROJECT.getData(anActionEvent.getDataContext());
        if (project == null) {
            notifyError(Constant.ERROR_NO_SELECTED_PROJECT);
            return;
        }
        String notificationContent = "";
        String artifactName = "<artifact>";
        List<MavenProject> mavenProjects = MavenProjectsManager.getInstance(project).getRootProjects();
        if (mavenProjects.size() > 0) {
            artifactName = String.format("%s.%s",
                    mavenProjects.get(0).getFinalName(), mavenProjects.get(0).getPackaging());
        }
        // create docker file
        try {
            DockerUtil.createDockerFile(project, Constant.DOCKER_CONTEXT_FOLDER, Constant.DOCKERFILE_NAME,
                    String.format(Constant.DOCKERFILE_CONTENT_TOMCAT, artifactName));
        } catch (IOException e) {
            e.printStackTrace();
            notifyError(e.getMessage());
            return;
        }
        // detect docker daemon
        notificationContent += String.format(Constant.MESSAGE_DOCKERFILE_CREATED,
                Paths.get(Constant.DOCKER_CONTEXT_FOLDER, Constant.DOCKERFILE_NAME)) + "\n";
        DefaultDockerClient.Builder builder = DockerRuntime.getInstance().getDockerBuilder();
        notificationContent += String.format(Constant.MESSAGE_DOCKER_HOST_INFO, builder.uri()) + "\n";
        // print instructions
        notificationContent += Constant.MESSAGE_ADD_DOCKER_SUPPORT_OK + "\n";
        notificationContent += Constant.MESSAGE_INSTRUCTION + "\n";
        notifyInfo(notificationContent);
    }

    @Override
    public void update(AnActionEvent event) {
        project = DataKeys.PROJECT.getData(event.getDataContext());
        boolean dockerFileExists = false;
        if (project != null) {
            String basePath = project.getBasePath();
            dockerFileExists = basePath != null && Paths.get(basePath, Constant.DOCKER_CONTEXT_FOLDER,
                    Constant.DOCKERFILE_NAME).toFile().exists();
        }
        event.getPresentation().setEnabledAndVisible(!dockerFileExists);
    }

    private void notifyInfo(String msg) {
        Notification notification = new Notification(NOTIFICATION_GROUP_ID, NOTIFICATION_TITLE,
                msg, NotificationType.INFORMATION);
        Notifications.Bus.notify(notification);
    }

    private void notifyError(String msg) {
        Notification notification = new Notification(NOTIFICATION_GROUP_ID, NOTIFICATION_TITLE,
                msg, NotificationType.ERROR);
        Notifications.Bus.notify(notification);
    }
}
