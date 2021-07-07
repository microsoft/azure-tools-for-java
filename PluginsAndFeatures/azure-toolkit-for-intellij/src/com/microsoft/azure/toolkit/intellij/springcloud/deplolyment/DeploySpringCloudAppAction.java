/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud.SpringCloudAppNode;

@Name("Deploy")
public class DeploySpringCloudAppAction extends NodeActionListener {
    private final Project project;
    private final SpringCloudApp app;

    public DeploySpringCloudAppAction(SpringCloudAppNode springCloudAppNode) {
        super();
        this.project = (Project) springCloudAppNode.getProject();
        this.app = springCloudAppNode.getApp();
    }

    @Override
    @AzureOperation(name = "springcloud.deploy", type = AzureOperation.Type.ACTION)
    protected void actionPerformed(NodeActionEvent nodeActionEvent) throws AzureCmdException {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> DeployAppAction.deployConfiguration(project, this.app));
    }
}
