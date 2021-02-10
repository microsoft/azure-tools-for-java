/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm.action;

import com.microsoft.azure.toolkit.intellij.arm.ExportTemplate;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;

@Name("Export Parameter File")
public class ExportParameterAction extends NodeActionListener {
    private final DeploymentNode deploymentNode;

    public ExportParameterAction(DeploymentNode deploymentNode) {
        this.deploymentNode = deploymentNode;
    }

    @Override
    protected void actionPerformed(NodeActionEvent nodeActionEvent) {
        ExportTemplate exportTemplate = new ExportTemplate(deploymentNode);
        exportTemplate.doExportParameters();
    }
}
