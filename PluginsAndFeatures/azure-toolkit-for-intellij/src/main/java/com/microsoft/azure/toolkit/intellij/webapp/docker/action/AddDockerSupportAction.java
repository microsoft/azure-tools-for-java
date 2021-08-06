/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp.docker.action;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.intellij.AzureAnAction;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azure.toolkit.intellij.webapp.docker.utils.Constant;
import com.microsoft.azure.toolkit.intellij.webapp.docker.utils.DockerUtil;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class AddDockerSupportAction extends AzureAnAction {
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";
    private static final String NOTIFICATION_TITLE = "Add Docker Support";
    private Module module;
    String pomXmlBasePath;

    @Override
    @AzureOperation(name = "docker.add_docker_support.configuration", type = AzureOperation.Type.ACTION)
    public boolean onActionPerformed(@NotNull AnActionEvent anActionEvent, @Nullable Operation operation) {
        module = DataKeys.MODULE.getData(anActionEvent.getDataContext());
        if (module == null) {
            notifyError(Constant.ERROR_NO_SELECTED_PROJECT);
            return true;
        }
        pomXmlBasePath = Paths.get(module.getModuleFilePath()).getParent().toString();
        String artifactRelativePath = Constant.DOCKERFILE_ARTIFACT_PLACEHOLDER;
        String dockerFileContent = Constant.DOCKERFILE_CONTENT_TOMCAT;
        List<MavenProject> mavenProjects = MavenProjectsManager.getInstance(module.getProject()).getProjects();
        Optional<MavenProject> res = mavenProjects.stream().filter(mvnprj ->
                Comparing.equal(Paths.get(mvnprj.getDirectory()).normalize(), Paths.get(pomXmlBasePath).normalize())
        ).findFirst();
        if (res.isPresent()) {
            MavenProject mvnPrj = res.get();
            String artifactName = mvnPrj.getFinalName() + "." + mvnPrj.getPackaging();
            artifactRelativePath = Paths.get(pomXmlBasePath).toUri()
                    .relativize(Paths.get(mvnPrj.getBuildDirectory(), artifactName).toUri())
                    .getPath();
            // pre-define dockerfile content according to artifact type
            if (MavenConstants.TYPE_WAR.equals(mvnPrj.getPackaging())) {
                // maven war: tomcat
                dockerFileContent = Constant.DOCKERFILE_CONTENT_TOMCAT;
            } else if (MavenConstants.TYPE_JAR.equals(mvnPrj.getPackaging())) {
                // maven jar: spring boot
                dockerFileContent = Constant.DOCKERFILE_CONTENT_SPRING;
            }
        }
        final Path path = Paths.get(pomXmlBasePath, Constant.DOCKERFILE_FOLDER, Constant.DOCKERFILE_NAME);
        try {
            // create docker file
            DockerUtil.createDockerFile(pomXmlBasePath, Constant.DOCKERFILE_FOLDER, Constant.DOCKERFILE_NAME,
                    String.format(dockerFileContent, artifactRelativePath));
            VirtualFileManager.getInstance().asyncRefresh(() -> {
                VirtualFile virtualDockerFile = LocalFileSystem.getInstance().findFileByPath(path.toString());
                if (virtualDockerFile != null) {
                    new OpenFileDescriptor(module.getProject(), virtualDockerFile).navigate(true);
                }
            });
        } catch (IOException e) {
            EventUtil.logError(operation, ErrorType.userError, e, null, null);
            e.printStackTrace();
            notifyError(e.getMessage());
            return true;
        }
        // detect docker daemon
        String defaultDockerHost = null;
        try {
            defaultDockerHost = DefaultDockerClient.fromEnv().uri().toString();
        } catch (DockerCertificateException e) {
            EventUtil.logError(operation, ErrorType.userError, e, null, null);
            e.printStackTrace();
            // leave defaultDockerHost null
        }
        // print instructions
        String notificationContent = "";
        notificationContent += String.format(Constant.MESSAGE_DOCKERFILE_CREATED, path.normalize()) + "\n";
        notificationContent += String.format(Constant.MESSAGE_DOCKER_HOST_INFO, defaultDockerHost) + "\n";
        notificationContent += Constant.MESSAGE_ADD_DOCKER_SUPPORT_OK + "\n";
        notificationContent += Constant.MESSAGE_INSTRUCTION + "\n";
        notifyInfo(notificationContent);
        return true;
    }

    protected String getServiceName(AnActionEvent event) {
        return TelemetryConstants.DOCKER;
    }

    protected String getOperationName(AnActionEvent event) {
        return TelemetryConstants.DEPLOY_DOCKER_HOST;
    }

    @Override
    public void update(AnActionEvent event) {
        module = DataKeys.MODULE.getData(event.getDataContext());
        boolean dockerFileExists = false;
        if (module != null) {
            String basePath = Paths.get(module.getModuleFilePath()).getParent().toString();
            dockerFileExists = basePath != null && Paths.get(basePath, Constant.DOCKERFILE_FOLDER,
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
