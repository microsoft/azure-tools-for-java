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
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.docker.utils.AzureDockerUIResources;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.docker.DockerHostNode;

@Name("Delete")
public class DeleteDockerHostAction extends NodeActionListener {
    private static final Logger log = Logger.getLogger(DeleteDockerHostAction.class.getName());
    DockerHost dockerHost;
    AzureDockerHostsManager dockerManager;
    IProject project;
    DockerHostNode dockerHostNode;

    public DeleteDockerHostAction(DockerHostNode dockerHostNode) {
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

        Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).azureClient;
        int option = AzureDockerUIResources.deleteAzureDockerHostConfirmationDialog(PluginUtil.getParentShell(), azureClient, dockerHost);

        if (option != 1 && option != 2) {
            if (AzureDockerUtils.DEBUG) System.out.format("User canceled delete Docker host op: %d\n", option);
            return;
        }

        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
            @Override
            public void run() {
              // instruct parent node to remove this node
              dockerHostNode.getParent().removeDirectChildNode(dockerHostNode);
            }
          });

        AzureDockerUIResources.deleteDockerHost(PluginUtil.getParentShell(), project, azureClient, dockerHost, option, new Runnable() {
            @Override
            public void run() {
                dockerManager.getDockerHostsList().remove(dockerHost);
//                dockerManager.refreshDockerHostDetails();
            }
          });

    }
}
