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

package com.microsoft.azuretools.azureexplorer.actions.docker;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.components.AzureWizardDialog;
import com.microsoft.azuretools.core.handlers.SignInCommandHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.docker.ui.wizards.createhost.AzureNewDockerWizard;
import com.microsoft.azuretools.docker.utils.AzureDockerUIResources;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.docker.DockerHostModule;

@Name("New Host")
public class CreateNewDockerHostAction extends NodeActionListener {
    private static final Logger log = Logger.getLogger(CreateNewDockerHostAction.class.getName());
    IProject project;
    DockerHostModule dockerHostModule;

    public CreateNewDockerHostAction(DockerHostModule dockerHostModule) {
        this.dockerHostModule = dockerHostModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent event) {
        try {
            if (!SignInCommandHandler.doSignIn(PluginUtil.getParentShell()))
                return;

            IProject project;
            Shell shell = PluginUtil.getParentShell();
            try {
                project = AzureDockerUIResources.getCurrentSelectedProject();
            } catch (Exception Ignored) {
                project = null;
            }
            if (project == null) {
                project = (IProject) dockerHostModule.getProject();
            }

            AzureManager azureAuthManager = AuthMethodManager.getInstance().getAzureManager();

            // not signed in
            if (azureAuthManager == null) {
                return;
            }

            AzureDockerHostsManager dockerManager = AzureDockerHostsManager
                    .getAzureDockerHostsManager(azureAuthManager);

            if (!dockerManager.isInitialized()) {
                AzureDockerUIResources.updateAzureResourcesWithProgressDialog(shell, project);
                if (AzureDockerUIResources.CANCELED) {
                    return;
                }
                dockerManager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(null);
            }

            if (dockerManager.getSubscriptionsMap().isEmpty()) {
                PluginUtil.displayErrorDialog(shell, "Create Docker Host", "Must select an Azure subscription first");
                return;
            }

            AzureNewDockerWizard newDockerWizard = new AzureNewDockerWizard(project, dockerManager);
            WizardDialog createNewDockerHostDialog = new AzureWizardDialog(shell, newDockerWizard);
            if (createNewDockerHostDialog.open() == Window.OK) {
                newDockerWizard.createHost();
            }

        } catch (Exception ex1) {
            log.log(Level.SEVERE, "actionPerformed", ex1);
            ex1.printStackTrace();
        }
    }
}

