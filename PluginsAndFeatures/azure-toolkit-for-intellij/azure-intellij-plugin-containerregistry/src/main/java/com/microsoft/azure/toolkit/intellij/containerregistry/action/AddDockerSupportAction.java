/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.microsoft.azure.toolkit.intellij.container.AzureDockerClient;
import com.microsoft.azure.toolkit.intellij.container.Constant;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"ComponentNotRegistered"})
public class AddDockerSupportAction extends AnAction {
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";
    private static final String NOTIFICATION_TITLE = "Add Docker Support";
    private Module module;
    String pomXmlBasePath;

    @Override
    @AzureOperation(name = "user/docker.add_docker_support")
    public void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
        module = PlatformDataKeys.MODULE.getData(anActionEvent.getDataContext());
        if (module == null) {
            AzureMessager.getMessager().error(Constant.ERROR_NO_SELECTED_PROJECT);
            return;
        }
        pomXmlBasePath = getModulePath(module);
        String artifactRelativePath = Constant.DOCKERFILE_ARTIFACT_PLACEHOLDER;
        String dockerFileContent = Constant.DOCKERFILE_CONTENT_TOMCAT;
        final List<MavenProject> mavenProjects = MavenProjectsManager.getInstance(module.getProject()).getProjects();
        final Optional<MavenProject> res = mavenProjects.stream().filter(mvnprj ->
            Comparing.equal(Paths.get(mvnprj.getDirectory()).normalize(), Paths.get(pomXmlBasePath).normalize())
        ).findFirst();
        if (res.isPresent()) {
            final MavenProject mvnPrj = res.get();
            final String artifactName = mvnPrj.getFinalName() + "." + mvnPrj.getPackaging();
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
        final Path path = Paths.get(pomXmlBasePath, Constant.DOCKERFILE_NAME);
        try {
            // create docker file
            AzureDockerClient.createDockerFile(pomXmlBasePath, Constant.DOCKERFILE_NAME,
                String.format(dockerFileContent, artifactRelativePath));
            VirtualFileManager.getInstance().asyncRefresh(() -> {
                final VirtualFile virtualDockerFile = LocalFileSystem.getInstance().findFileByPath(path.toString());
                if (virtualDockerFile != null) {
                    new OpenFileDescriptor(module.getProject(), virtualDockerFile).navigate(true);
                }
            });
        } catch (final IOException e) {
            AzureMessager.getMessager().error(e);
            return;
        }

        // print instructions
        String notificationContent = "";
        notificationContent += String.format(Constant.MESSAGE_DOCKERFILE_CREATED, path.normalize()) + "\n";
        notificationContent += Constant.MESSAGE_ADD_DOCKER_SUPPORT_OK + "\n";
        notificationContent += Constant.MESSAGE_INSTRUCTION + "\n";
        AzureMessager.getMessager().info(notificationContent);
    }

    protected String getServiceName(AnActionEvent event) {
        return TelemetryConstants.DOCKER;
    }

    protected String getOperationName(AnActionEvent event) {
        return TelemetryConstants.DEPLOY_DOCKER_HOST;
    }

    @Override
    @ExceptionNotification
    public void update(AnActionEvent event) {
        module = PlatformDataKeys.MODULE.getData(event.getDataContext());
        boolean dockerFileExists = false;
        if (module != null) {
            final String basePath = getModulePath(module);
            dockerFileExists = basePath != null && Paths.get(basePath, Constant.DOCKERFILE_NAME).toFile().exists();
        }
        event.getPresentation().setEnabledAndVisible(!dockerFileExists);
    }

    public static String getModulePath(Module module) {
        return Optional.of(ModuleRootManager.getInstance(module).getContentRoots())
            .filter(ArrayUtils::isNotEmpty)
            .map(array -> array[0].getPath()).orElse(null);
    }
}
