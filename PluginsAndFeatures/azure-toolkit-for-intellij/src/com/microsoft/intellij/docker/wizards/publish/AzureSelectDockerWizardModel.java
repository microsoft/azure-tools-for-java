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

package com.microsoft.intellij.docker.wizards.publish;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardModel;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.AzureDockerSubscription;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.intellij.docker.wizards.publish.forms.AzureConfigureDockerContainerStep;
import com.microsoft.intellij.docker.wizards.publish.forms.AzureSelectDockerHostStep;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.util.HashMap;
import java.util.Map;

public class AzureSelectDockerWizardModel extends WizardModel implements TelemetryProperties {
  private Project project;
  private AzureSelectDockerHostStep selectDockerHostForm;
  private AzureConfigureDockerContainerStep configureDockerContainerForm;
  private AzureSelectDockerWizardDialog selectDockerWizardDialog;

  private AzureDockerHostsManager dockerManager;

  private AzureDockerImageInstance dockerImageDescription;
  private AzureDockerSubscription subscription;
  public boolean finishedOK;

  public AzureSelectDockerWizardModel(final Project project, AzureDockerHostsManager uiManager, AzureDockerImageInstance dockerImageInstance) {
    super("");
    this.project = project;
    this.dockerManager = uiManager;
    this.dockerImageDescription = dockerImageInstance;
    this.finishedOK = true;

    configureDockerContainerForm = new AzureConfigureDockerContainerStep(this.getTitle(), this, uiManager, dockerImageInstance);
    selectDockerHostForm = new AzureSelectDockerHostStep(this.getTitle(), this, uiManager, dockerImageInstance);
    add(selectDockerHostForm);
    add(configureDockerContainerForm);
  }

  public void setPredefinedDockerfileOptions(String artifactFileName) {
    if (configureDockerContainerForm != null) {
      configureDockerContainerForm.setPredefinedDockerfileOptions(artifactFileName);
    }
  }

  public void setDockerContainerName(String dockerContainerName) {
    if (configureDockerContainerForm != null) {
      configureDockerContainerForm.setDockerContainerName(dockerContainerName);
    }
  }

  public void setDockerUIManager(AzureDockerHostsManager manager) {
    dockerManager = manager;
  }

  public void setSelectDockerWizardDialog(AzureSelectDockerWizardDialog dialog) {
    selectDockerWizardDialog = dialog;
  }

  public AzureSelectDockerWizardDialog getSelectDockerWizardDialog() {
    return selectDockerWizardDialog;
  }

  public AzureDockerHostsManager getDockerHostsManager() {
    return dockerManager;
  }

  public Project getProject() {
    return project;
  }

  public AzureDockerImageInstance getDockerImageDescription() {
    return dockerImageDescription;
  }

  public ValidationInfo doValidate() {
    ValidationInfo validationInfo = selectDockerHostForm.doValidate();
    if (validationInfo != null) {
      finishedOK = false;
      return validationInfo;
    }
    validationInfo = configureDockerContainerForm.doValidate();
    if (validationInfo != null) {
      finishedOK = false;
      return validationInfo;
    }
    if (!dockerImageDescription.hasNewDockerHost &&
        dockerImageDescription.host.state != DockerHost.DockerHostVMState.RUNNING &&
        !DefaultLoader.getUIHelper().showConfirmation(String.format("The selected Docker host %s state is %s and publishing could fail.\n\n Do you want to continue?", dockerImageDescription.host.name, dockerImageDescription.host.state),
            "Docker Host Not in Running State", new String[]{"Yes", "No"}, null)) {
      ValidationInfo info = new ValidationInfo("Invalid Docker host selection", selectDockerHostForm.getPreferredFocusedComponent());
      finishedOK = false;
      return info;
    }
    finishedOK = true;

    return null;
  }

  public void selectDefaultDockerHost(DockerHost dockerHost, boolean selectOtherHosts) {
    if (selectDockerHostForm != null) {
      selectDockerHostForm.selectDefaultDockerHost(dockerHost, selectOtherHosts);
    }
  }

  public void setSubscription(AzureDockerSubscription subscription) { this.subscription = subscription; }

  @Override
  public Map<String, String> toProperties() {
    final Map<String, String> properties = new HashMap<>();

    if(this.subscription != null) {
        properties.put(AppInsightsConstants.SubscriptionName, this.subscription.name);
        properties.put(AppInsightsConstants.SubscriptionId, this.subscription.id);
    }

    return properties;
  }

}
