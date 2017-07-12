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

/**
 * Created by yanzh on 7/10/2017.
 */
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
