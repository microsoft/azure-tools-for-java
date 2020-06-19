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
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.intellij.docker.utils.AzureDockerUIResources;
import com.microsoft.intellij.util.AzureLoginHelper;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.docker.DockerHostModule;

@Name("Publish")
public class PublishDockerContainerAction extends NodeActionListener {
    private static final Logger LOGGER = Logger.getInstance(PublishDockerContainerAction.class);
    private static final String ERROR_PUBLISHING_DOCKER_CONTAINER = "Error publishing docker container";

    private Project project;

    public PublishDockerContainerAction(DockerHostModule dockerHostModule) {
        this.project = (Project) dockerHostModule.getProject();
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        try {
            if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) {
                return;
            }
            if (!AzureLoginHelper.isAzureSubsAvailableOrReportError(ERROR_PUBLISHING_DOCKER_CONTAINER)) {
                return;
            }
            AzureDockerUIResources.publish2DockerHostContainer(project);
        } catch (Exception ex1) {
            LOGGER.error("actionPerformed", ex1);
            ex1.printStackTrace();
        }
    }
}
