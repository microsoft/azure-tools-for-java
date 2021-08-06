/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm.action;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.azure.toolkit.intellij.arm.UpdateDeploymentForm;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.actions.AzureSignInAction;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;

@Name("Update Deployment")
public class UpdateDeploymentAction extends NodeActionListener {

    private final DeploymentNode deploymentNode;
    public static final String NOTIFY_UPDATE_DEPLOYMENT_SUCCESS = "Update deployment successfully";
    public static final String NOTIFY_UPDATE_DEPLOYMENT_FAIL = "Update deployment failed";

    public UpdateDeploymentAction(DeploymentNode deploymentNode) {
        this.deploymentNode = deploymentNode;
    }

    @Override
    protected void actionPerformed(NodeActionEvent nodeActionEvent) {
        Project project = (Project) deploymentNode.getProject();
        AzureSignInAction.requireSignedIn(project, () -> this.doActionPerformed(nodeActionEvent, true, project));
    }

    private void doActionPerformed(NodeActionEvent e, boolean isLoggedIn, Project project) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        try {
            if (isLoggedIn) {
                UpdateDeploymentForm updateDeploymentForm = new UpdateDeploymentForm(project, deploymentNode);
                updateDeploymentForm.show();
            }
        } catch (Exception ex) {
            AzurePlugin.log("Error Update Deployment", ex);
            UIUtils.showNotification(statusBar, NOTIFY_UPDATE_DEPLOYMENT_FAIL + ", " + ex.getMessage(),
                MessageType.ERROR);
        }
    }
}
