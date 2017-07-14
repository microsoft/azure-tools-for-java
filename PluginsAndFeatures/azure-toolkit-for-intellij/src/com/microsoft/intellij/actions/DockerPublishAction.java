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
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.azuretools.ijidea.utility.AzureAnAction;
import com.microsoft.intellij.container.ConsoleLogger;
import com.microsoft.intellij.container.Constant;
import com.microsoft.intellij.container.DockerRuntime;
import com.microsoft.intellij.container.ui.PublishWizardDialog;
import com.microsoft.intellij.container.ui.PublishWizardModel;
import com.microsoft.intellij.container.utils.ConfigFileUtil;
import com.microsoft.intellij.container.utils.DockerUtil;
import com.microsoft.intellij.container.utils.WarUtil;
import com.spotify.docker.client.DockerClient;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class DockerPublishAction extends AzureAnAction {
    @Override
    public void onActionPerformed(AnActionEvent anActionEvent) {
        Project project = DataKeys.PROJECT.getData(anActionEvent.getDataContext());
        JFrame frame = WindowManager.getInstance().getFrame(project);
        try {
            if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Properties props = ConfigFileUtil.loadConfig(project);
        DockerRuntime.getInstance().loadFromProps(props);
        DockerClient dockerClient = DockerRuntime.getInstance().getDockerBuilder().build();
        try {
            Path destinationPath = Paths.get(project.getBasePath(), Constant.DOCKER_CONTEXT_FOLDER, project.getName() + ".war");
            WarUtil.export(project, destinationPath);
            DockerUtil.buildImage(dockerClient, project, Paths.get(project.getBasePath(), Constant.DOCKER_CONTEXT_FOLDER));
        } catch (Exception e) {
            e.printStackTrace();
        }
        DockerRuntime.getInstance().setLatestArtifactName(project.getName());
        PublishWizardDialog pwd = new PublishWizardDialog(true, true, new PublishWizardModel());
        pwd.show();
        if (pwd.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            ConsoleLogger.info(String.format("URL: http://%s.azurewebsites.net/%s",
                    DockerRuntime.getInstance().getLatestWebAppName(), project.getName()));
            props = DockerRuntime.getInstance().saveToProps(props);
            ConfigFileUtil.saveConfig(project, props);
        }

    }
}
