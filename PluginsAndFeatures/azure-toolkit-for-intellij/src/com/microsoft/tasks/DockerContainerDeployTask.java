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

package com.microsoft.tasks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.activitylog.ActivityLogToolWindowFactory;
import com.microsoft.intellij.deploy.AzureDeploymentProgressNotification;
import com.microsoft.intellij.ui.messages.AzureBundle;
import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventArgs;
import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventListener;
import org.jetbrains.annotations.NotNull;

public class DockerContainerDeployTask extends Task.Backgroundable {
  String url;
  Project project;
  AzureDockerImageInstance dockerImageInstance;
  Azure azureClient;

  static final Logger LOG = Logger.getInstance(DockerContainerDeployTask.class);

  public DockerContainerDeployTask(Project project, Azure azureClient, AzureDockerImageInstance dockerImageInstance) {
    super(project, "Publishing Docker Container on Azure", true, Backgroundable.DEAF);
    this.project = project;
    this.dockerImageInstance = dockerImageInstance;
    this.azureClient = azureClient;

    this.url = AzureDockerUtils.getUrl(dockerImageInstance);
  }

  @Override
  public void run(@NotNull final ProgressIndicator indicator) {
    openViews(project);
    AzurePlugin.removeUnNecessaryListener();
    DeploymentEventListener deployListnr = new DeploymentEventListener() {
      @Override
      public void onDeploymentStep(DeploymentEventArgs args) {
        indicator.setFraction(args.getDeployCompleteness() / 100.0);
        indicator.setText(AzureBundle.message("deployingToAzure"));
        indicator.setText2(args.toString());
      }
    };
    AzurePlugin.addDeploymentEventListener(deployListnr);
    AzurePlugin.depEveList.add(deployListnr);
    new AzureDeploymentProgressNotification(project).deployToDockerContainer(dockerImageInstance, url);

    new Thread("Warm up the target site") {
      public void run() {
        try {

          LOG.info("To warm the site up - implicitly trying to connect it");
          WebAppUtils.sendGet(url);
        }
        catch (Exception ex) {
          LOG.info(ex.getMessage(), ex);
        }
      }
    }.start();

  }

  private void openViews(final Project project) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
//        ToolWindowManager.getInstance(project).getToolWindow(ServerExplorerToolWindowFactory.EXPLORER_WINDOW).activate(null);
        ToolWindowManager.getInstance(project).getToolWindow(ActivityLogToolWindowFactory.ACTIVITY_LOG_WINDOW).activate(null);

      }
    });
  }
}
