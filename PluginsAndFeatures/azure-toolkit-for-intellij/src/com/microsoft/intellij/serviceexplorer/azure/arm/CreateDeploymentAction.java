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

package com.microsoft.intellij.serviceexplorer.azure.arm;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.forms.arm.CreateDeploymentForm;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.intellij.util.AzureLoginHelper;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.ResourceManagementModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.ResourceManagementNode;

@Name("Create Deployment")
public class CreateDeploymentAction extends NodeActionListener {
    public static final String ERROR_CREATING_DEPLOYMENT = "Error creating Deployment";
    private final Project project;
    private final Node node;
    public static final String NOTIFY_CREATE_DEPLOYMENT_SUCCESS = "Create deployment successfully";
    public static final String NOTIFY_CREATE_DEPLOYMENT_FAIL = "Create deployment failed";

    public CreateDeploymentAction(ResourceManagementModule module) {
        this.project = (Project) module.getProject();
        this.node = module;
    }

    public CreateDeploymentAction(ResourceManagementNode node) {
        this.project = (Project) node.getProject();
        this.node = node;
    }

    @Override
    protected void actionPerformed(NodeActionEvent nodeActionEvent) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        try {
            if (AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) {
                if (!AzureLoginHelper.isAzureSubsAvailableOrReportError(ERROR_CREATING_DEPLOYMENT)) {
                    return;
                }
                CreateDeploymentForm createDeploymentForm = new CreateDeploymentForm(project);
                if (node instanceof ResourceManagementNode) {
                    ResourceManagementNode rmNode = (ResourceManagementNode) node;
                    createDeploymentForm.filleSubsAndRg(rmNode);
                }
                createDeploymentForm.show();
            }
        } catch (Exception ex) {
            AzurePlugin.log(ERROR_CREATING_DEPLOYMENT, ex);
            UIUtils.showNotification(statusBar, NOTIFY_CREATE_DEPLOYMENT_FAIL + ", " + ex.getMessage(),
                                     MessageType.ERROR);
        }
    }
}
