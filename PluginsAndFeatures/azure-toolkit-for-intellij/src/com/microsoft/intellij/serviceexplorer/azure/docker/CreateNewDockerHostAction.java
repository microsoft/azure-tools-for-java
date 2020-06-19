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

package com.microsoft.intellij.serviceexplorer.azure.docker;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.intellij.docker.utils.AzureDockerUIResources;
import com.microsoft.intellij.docker.wizards.createhost.AzureNewDockerWizardDialog;
import com.microsoft.intellij.docker.wizards.createhost.AzureNewDockerWizardModel;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.docker.DockerHostModule;

import static com.microsoft.intellij.util.AzureLoginHelper.MUST_SELECT_SUBSCRIPTION;

@Name("New Host")
public class CreateNewDockerHostAction extends NodeActionListener {
    private static final Logger LOGGER = Logger.getInstance(CreateNewDockerHostAction.class);
    DockerHost dockerHost;
    AzureDockerHostsManager dockerManager;
    Project project;
    DockerHostModule dockerHostModule;

    public CreateNewDockerHostAction(DockerHostModule dockerHostModule) {
        this.project = (Project) dockerHostModule.getProject();
        this.dockerHostModule = dockerHostModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        try {
            if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) {
                return;
            }
            AzureManager azureAuthManager = AuthMethodManager.getInstance().getAzureManager();

            // not signed in
            if (azureAuthManager == null) {
                return;
            }

            AzureDockerHostsManager manager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(
                    azureAuthManager);

            if (!manager.isInitialized()) {
                AzureDockerUIResources.updateAzureResourcesWithProgressDialog(project);
                if (AzureDockerUIResources.CANCELED) {
                    return;
                }
                manager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(null);
            }

            if (manager.getSubscriptionsMap().isEmpty()) {
                PluginUtil.displayErrorDialog("Create Docker Host", MUST_SELECT_SUBSCRIPTION);
                return;
            }

            AzureNewDockerWizardModel newDockerHostModel = new AzureNewDockerWizardModel(project, manager);
            AzureNewDockerWizardDialog wizard = new AzureNewDockerWizardDialog(newDockerHostModel);
            wizard.setTitle("Create Docker Host");
            wizard.show();

            if (wizard.getExitCode() == 0) {
                dockerHost = newDockerHostModel.getDockerHost();
                wizard.create();
                System.out.println("New Docker host will be created at: " + dockerHost.apiUrl);
            }
        } catch (Exception ex1) {
            ex1.printStackTrace();
        }
    }
}
