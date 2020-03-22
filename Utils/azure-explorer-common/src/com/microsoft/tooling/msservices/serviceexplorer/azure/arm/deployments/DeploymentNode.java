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
package com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments;

import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.ResourceManagementNode;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.ARM;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SHOW_DEPLOYMENT_PROPERTY;

public class DeploymentNode extends Node implements DeploymentNodeView {

    public static final String ICON_PATH = "arm_deployment.png";
    private static final String EXPORT_TEMPLATE_SUCCESS = "Export successfully.";
    private static final String EXPORT_TEMPLATE_FAIL = "MS Services - Error Export resource manager template";
    private static final String SHOW_PROPERTY_ACTION = "Show Properties";
    private static final String DELETE_ACTION = "Delete";
    private static final String DELETE_DEPLOYMENT_PROMPT_MESSAGE = "This operation will delete the Deployment "
        + "%s. Are you sure you want to continue?";
    private static final String DELETE_DEPLOYMENT_PROGRESS_MESSAGE = "Deleting Deployment";
    private final Deployment deployment;
    private final DeploymentNodePresenter deploymentNodePresenter;
    private final String subscriptionId;

    public DeploymentNode(ResourceManagementNode parent, String subscriptionId, Deployment deployment) {
        super(deployment.id(), deployment.name(), parent, ICON_PATH, true);
        this.deployment = deployment;
        this.subscriptionId = subscriptionId;
        deploymentNodePresenter = new DeploymentNodePresenter();
        deploymentNodePresenter.onAttachView(this);
        loadActions();
    }

    @Override
    public void showExportTemplateResult(boolean isSuccess, Throwable t) {
        if (isSuccess) {
            DefaultLoader.getUIHelper().showInfo(this, EXPORT_TEMPLATE_SUCCESS);
        } else {
            DefaultLoader.getUIHelper().showException(t.getMessage(), t, EXPORT_TEMPLATE_FAIL, false, true);
        }
    }

    @Override
    protected void loadActions() {
        addAction(SHOW_PROPERTY_ACTION, null, new ShowDeploymentPropertyAction());
        addAction(DELETE_ACTION, null, new DeleteDeploymentAction());
        super.loadActions();
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public DeploymentNodePresenter getDeploymentNodePresenter() {
        return deploymentNodePresenter;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    // Show property action class
    private class ShowDeploymentPropertyAction extends NodeActionListener {

        @Override
        protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
            EventUtil.logEvent(EventType.info, ARM, SHOW_DEPLOYMENT_PROPERTY, null);
            DefaultLoader.getUIHelper().openDeploymentPropertyView(DeploymentNode.this);
        }
    }

    private class DeleteDeploymentAction extends AzureNodeActionPromptListener {

        DeleteDeploymentAction() {
            super(DeploymentNode.this, String.format(DELETE_DEPLOYMENT_PROMPT_MESSAGE, deployment.name()),
                DELETE_DEPLOYMENT_PROGRESS_MESSAGE);
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e) {
            getParent().removeNode(subscriptionId, deployment.id(), DeploymentNode.this);
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) {
        }
    }
}
