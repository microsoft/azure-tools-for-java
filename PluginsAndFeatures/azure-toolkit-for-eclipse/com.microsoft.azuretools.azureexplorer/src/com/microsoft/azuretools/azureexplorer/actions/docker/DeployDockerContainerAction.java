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

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.handlers.SignInCommandHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.docker.ui.wizards.publish.AzureSelectDockerWizard;
import com.microsoft.azuretools.docker.utils.AzureDockerUIResources;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.docker.DockerHostNode;

@Name("Publish")
public class DeployDockerContainerAction extends NodeActionListener {
    private static final Logger log = Logger.getLogger(DeployDockerContainerAction.class.getName());
    DockerHost dockerHost;
    AzureDockerHostsManager dockerManager;
    IProject project;
    DockerHostNode dockerHostNode;

    public DeployDockerContainerAction(DockerHostNode dockerHostNode) {
        this.dockerManager = dockerHostNode.getDockerManager();
        this.dockerHost = dockerHostNode.getDockerHost();
        this.dockerHostNode = dockerHostNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        try {
            if (!SignInCommandHandler.doSignIn(PluginUtil.getParentShell()))
                return;

            IProject project;
            try {
                project = AzureDockerUIResources.getCurrentSelectedProject();
            } catch (Exception Ignored) {
                project = null;
            }
            if (project == null) {
                project = (IProject) dockerHostNode.getProject();
            }

            AzureDockerUIResources.publish2DockerHostContainer(PluginUtil.getParentShell(), project, dockerHost);

//            AzureDockerUIResources.createArtifact(PluginUtil.getParentShell(), project);
//
//            AzureManager azureAuthManager = AuthMethodManager.getInstance().getAzureManager();
//
//            // not signed in
//            if (azureAuthManager == null) {
//                return;
//            }
//
//            AzureDockerHostsManager dockerManager = AzureDockerHostsManager.getAzureDockerHostsManager(azureAuthManager);
//
//            if (!dockerManager.isInitialized()) {
//                AzureDockerUIResources.updateAzureResourcesWithProgressDialog(PluginUtil.getParentShell(), project);
//                if (AzureDockerUIResources.CANCELED) {
//                    return;
//                }
//                dockerManager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(null);
//            }
//
//            if (dockerManager.getSubscriptionsMap().isEmpty()) {
//                PluginUtil.displayErrorDialog(PluginUtil.getParentShell(), "Create Docker Host", "Must select an Azure subscription first");
//                return;
//            }
//
//            AzureDockerImageInstance dockerImageDescription = dockerManager.getDefaultDockerImageDescription(project.getName(), dockerHost);
//            AzureSelectDockerWizard selectDockerWizard = new AzureSelectDockerWizard(project, dockerManager, dockerImageDescription);
//            WizardDialog selectDockerHostDialog = new WizardDialog(PluginUtil.getParentShell(), selectDockerWizard);
//            selectDockerWizard.selectDefaultDockerHost(dockerHost, false);
//
//            if (selectDockerHostDialog.open() == Window.OK) {
//                try {
//                    String url = selectDockerWizard.deploy();
//                    if (AzureDockerUtils.DEBUG) System.out.println("Web app published at: " + url);
//                  } catch (Exception ex) {
//                    PluginUtil.displayErrorDialogAndLog(PluginUtil.getParentShell(), "Unexpected error detected while publishing to a Docker container", ex.getMessage(), ex);
//                    log.log(Level.SEVERE, "publish2DockerHostContainer: " + ex.getMessage(), ex);
//                  }
//            }
//
        } catch (Exception ex1) {
            log.log(Level.SEVERE, "actionPerformed", ex1);
            ex1.printStackTrace();
        }
    }
}
