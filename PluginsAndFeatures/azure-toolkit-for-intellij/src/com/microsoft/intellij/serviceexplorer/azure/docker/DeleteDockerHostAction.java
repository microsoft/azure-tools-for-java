/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.management.Azure;
import com.microsoft.intellij.docker.utils.AzureDockerUIResources;
import com.microsoft.intellij.docker.wizards.publish.forms.CalculateDockerDeletionSafenessBackgroundTask;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.docker.DockerHostNode;
import org.jetbrains.concurrency.AsyncPromise;


@Name("Delete")
public class DeleteDockerHostAction extends NodeActionListener {
    private static final Logger LOGGER = Logger.getInstance(DeleteDockerHostAction.class);
    DockerHost dockerHost;
    AzureDockerHostsManager dockerManager;
    Project project;
    DockerHostNode dockerHostNode;

    public DeleteDockerHostAction(DockerHostNode dockerHostNode) {
        this.dockerManager = dockerHostNode.getDockerManager();
        this.dockerHost = dockerHostNode.getDockerHost();
        this.project = (Project) dockerHostNode.getProject();
        this.dockerHostNode = dockerHostNode;

    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        dockerHost = dockerManager.getDockerHostForURL(dockerHost.apiUrl);
        Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).azureClient;

        AsyncPromise<Boolean> promise = new AsyncPromise<>();
        Task task = new CalculateDockerDeletionSafenessBackgroundTask(promise, azureClient, dockerHost);
        promise.onSuccess(isDeletingSafe ->
        {
            try {
                int option = AzureDockerUIResources.deleteAzureDockerHostConfirmationDialog(dockerHost, isDeletingSafe);

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

                AzureDockerUIResources.deleteDockerHost(project, azureClient, dockerHost, option, new Runnable() {
                    @Override
                    public void run() {
                        dockerManager.getDockerHostsList().remove(dockerHost);
                    }
                });
            } catch (Exception ex) {
                DefaultLoader.getUIHelper().logError("DeleteDockerHostAction", ex);
            }
        });

        ProgressManager.getInstance().run(task);
    }
}

