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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.PlatformUtils;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.azuretools.ijidea.utility.AzureAnAction;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.docker.utils.AzureDockerUIResources;
import com.microsoft.intellij.docker.wizards.publish.AzureSelectDockerWizardDialog;
import com.microsoft.intellij.docker.wizards.publish.AzureSelectDockerWizardModel;
import com.microsoft.intellij.util.PluginUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import static com.intellij.projectImport.ProjectImportBuilder.getCurrentProject;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;


public class AzureDockerHostDeployAction extends AzureAnAction {
  private static final Logger LOGGER = Logger.getInstance(AzureDockerHostDeployAction.class);

    public boolean onActionPerformed(@NotNull AnActionEvent anActionEvent, @Nullable Operation operation) {
    try {
        Project project = getCurrentProject();
        if (!AzureSignInAction.doSignIn( AuthMethodManager.getInstance(), project)) return true;
        AzureDockerUIResources.CANCELED = false;

        Module module = PluginUtil.getSelectedModule();
        List<Module> modules = Arrays.asList(ModuleManager.getInstance(project).getModules());

        if (module == null && modules.isEmpty()) {
        Messages.showErrorDialog(message("noModule"), message("error"));
        } else if (module == null) {
        module = modules.iterator().next();
        }

        AzureManager azureAuthManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureAuthManager == null) {
        System.out.println("ERROR! Not signed in!");
        return true;
        }


        AzureDockerHostsManager dockerManager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(azureAuthManager);

        if (!dockerManager.isInitialized()) {
        AzureDockerUIResources.updateAzureResourcesWithProgressDialog(project);
        if (AzureDockerUIResources.CANCELED) {
          return true;
        }
      }

      if (dockerManager.getSubscriptionsMap().isEmpty()) {
        PluginUtil.displayErrorDialog("Publish Docker Container", "Please select a subscription first");
        return true;
      }

      DockerHost dockerHost = (dockerManager.getDockerPreferredSettings() != null) ? dockerManager.getDockerHostForURL(dockerManager.getDockerPreferredSettings().dockerApiName) : null;
      AzureDockerImageInstance dockerImageDescription = dockerManager.getDefaultDockerImageDescription(project.getName(), dockerHost);

      AzureSelectDockerWizardModel model = new AzureSelectDockerWizardModel(project, dockerManager, dockerImageDescription);
      AzureSelectDockerWizardDialog wizard = new AzureSelectDockerWizardDialog(model);
      if (dockerHost != null) {
        model.selectDefaultDockerHost(dockerHost, true);
      }
      wizard.show();

      if (wizard.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
        try {
          String url = wizard.deploy();
          System.out.println("Web app published at: " + url);
        } catch (Exception ex) {
          PluginUtil.displayErrorDialogAndLog(message("webAppDplyErr"), ex.getMessage(), ex);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return true;
  }

    @Override
    protected String getServiceName(AnActionEvent event) {
        return TelemetryConstants.DOCKER;
    }

    @Override
    protected String getOperationName(AnActionEvent event) {
        return TelemetryConstants.CREATE_DOCKER_HOST;
    }


  @Override
  public void update(AnActionEvent actionEvent) {
    final Module module = actionEvent.getData(LangDataKeys.MODULE);
    actionEvent.getPresentation().setVisible(PlatformUtils.isIdeaUltimate());
    if (!PlatformUtils.isIdeaUltimate()) {
      return;
    }

//    try {
//      boolean isSignIn = AuthMethodManager.getInstance().isSignedIn();
//      boolean isEnabled = isSignIn & module != null && ModuleTypeId.JAVA_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE));
//      actionEvent.getPresentation().setEnabled(isEnabled);
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
  }

}