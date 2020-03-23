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

import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.docker.ui.dialogs.AzureViewDockerDialog;
import com.microsoft.azuretools.docker.utils.AzureDockerUIResources;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.docker.DockerHostNode;

@Name("Details")
public class ViewDockerHostAction extends NodeActionListener {
    private static final Logger log = Logger.getLogger(ViewDockerHostAction.class.getName());
    DockerHost dockerHost;
    AzureDockerHostsManager dockerManager;
    IProject project;
    DockerHostNode dockerHostNode;

    public ViewDockerHostAction(DockerHostNode dockerHostNode) {
        this.dockerManager = dockerHostNode.getDockerManager();
        this.dockerHost = dockerHostNode.getDockerHost();
        this.dockerHostNode = dockerHostNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        IProject project;
        try {
            project = AzureDockerUIResources.getCurrentSelectedProject();
        } catch (Exception Ignored) {
            project = null;
        }
        if (project == null) {
            project = (IProject) dockerHostNode.getProject();
        }

        AzureViewDockerDialog viewDockerDialog = new AzureViewDockerDialog(PluginUtil.getParentShell(), project, dockerHost, dockerManager);
        viewDockerDialog.open();

        if (viewDockerDialog.getInternalExitCode() == AzureViewDockerDialog.UPDATE_EXIT_CODE) {
            if (dockerHost != null && !dockerHost.isUpdating) {
                AzureDockerUIResources.updateDockerHost(PluginUtil.getParentShell(), project, new EditableDockerHost(dockerHost), dockerManager, true);
            } else {
                PluginUtil.displayErrorDialog(PluginUtil.getParentShell(), "Error: Invalid Edit Selection", "The selected Docker host can not be edited at this time!");
            }
        }
    }
}
