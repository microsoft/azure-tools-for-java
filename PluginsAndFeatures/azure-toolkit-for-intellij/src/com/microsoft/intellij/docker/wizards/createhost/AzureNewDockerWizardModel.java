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

package com.microsoft.intellij.docker.wizards.createhost;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardModel;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerSubscription;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.intellij.docker.wizards.createhost.forms.AzureNewDockerHostStep;
import com.microsoft.intellij.docker.wizards.createhost.forms.AzureNewDockerLoginStep;
import com.microsoft.azuretools.telemetry.TelemetryProperties;

import java.util.HashMap;
import java.util.Map;

public class AzureNewDockerWizardModel extends WizardModel implements TelemetryProperties {
  private Project project;

  private AzureNewDockerHostStep newDockerHostStep;
  private AzureNewDockerLoginStep newDockerLoginStep;
  private AzureNewDockerWizardDialog newDockerWizardDialog;

  private AzureDockerHostsManager dockerManager;
  public DockerHost newHost;
  public boolean finishedOK;
  private AzureDockerSubscription subscription;

  public AzureNewDockerWizardModel(final Project project, AzureDockerHostsManager dockerManager) {
//    super("Create a virtual machine as a Docker host");
    super("");
    this.project = project;
    this.dockerManager = dockerManager;
    this.finishedOK = true;

    newHost = dockerManager.createNewDockerHostDescription(AzureDockerUtils.getDefaultRandomName(AzureDockerUtils.getDefaultName(project.getName())));
    newDockerHostStep = new AzureNewDockerHostStep(this.getTitle(), this);
    newDockerLoginStep = new AzureNewDockerLoginStep(this.getTitle(), this);
    add(newDockerHostStep);
    add(newDockerLoginStep);
  }

  public DockerHost getDockerHost() {
    return newHost;
  }

  public void setNewDockerHost(DockerHost dockerHost) {
    newHost = dockerHost;
  }

  public Project getProject() {
    return project;
  }

  public ValidationInfo doValidate() {
    ValidationInfo validationInfo = newDockerHostStep.doValidate();
    if (validationInfo != null) {
      finishedOK = false;
      return validationInfo;
    }
    validationInfo = newDockerLoginStep.doValidate();
    if (validationInfo != null) {
      finishedOK = false;
      return validationInfo;
    }
    finishedOK = true;
    return null;
  }

  public boolean canClose() { return finishedOK;}

  public void setDockerManager(AzureDockerHostsManager manager) {
    dockerManager = manager;
  }

  public void setNewDockerWizardDialog(AzureNewDockerWizardDialog dialog) {
    newDockerWizardDialog = dialog;
  }

  public AzureNewDockerWizardDialog getNewDockerWizardDialog() {
    return newDockerWizardDialog;
  }

  public AzureDockerHostsManager getDockerManager() {
    return dockerManager;
  }

  public void DialogShaker(ValidationInfo info) {
    newDockerWizardDialog.DialogShaker(info);
  }

  public void setSubscription(AzureDockerSubscription subscription) { this.subscription = subscription; }

  public AzureDockerSubscription getSubscription() { return subscription; }

  @Override
  public Map<String, String> toProperties() {
    final Map<String, String> properties = new HashMap<>();

    if(this.subscription != null) {
      properties.put("SubscriptionName", this.getSubscription().name);
      properties.put("SubscriptionId", this.getSubscription().id);
    }

    return properties;
  }

}
