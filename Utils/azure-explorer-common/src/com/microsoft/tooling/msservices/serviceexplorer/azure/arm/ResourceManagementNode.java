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

package com.microsoft.tooling.msservices.serviceexplorer.azure.arm;

import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.BasicActionBuilder;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;

import java.util.List;

public class ResourceManagementNode extends RefreshableNode implements ResourceManagementNodeView {

    private final ResourceManagementNodePresenter rmNodePresenter;
    private final String sid;
    private final String rgName;

    public ResourceManagementNode(ResourceManagementModule parent, String subscriptionId, ResourceGroup resourceGroup) {
        super(resourceGroup.id(), resourceGroup.name(), parent, null, true);
        rmNodePresenter = new ResourceManagementNodePresenter();
        rmNodePresenter.onAttachView(this);
        sid = subscriptionId;
        rgName = resourceGroup.name();
        loadActions();
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.ResourceManagement.MODULE;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        rmNodePresenter.onModuleRefresh(sid, rgName);
    }

    @Override
    protected void loadActions() {
        addAction(initActionBuilder(this::delete).withAction(AzureActionEnum.DELETE).withBackgroudable(true).withPromptable(true).build());
        super.loadActions();
    }

    protected final BasicActionBuilder initActionBuilder(Runnable runnable) {
        return new BasicActionBuilder(runnable)
                .withModuleName(ResourceManagementModule.MODULE_NAME)
                .withInstanceName(name);
    }

    @Override
    public void renderChildren(List<ResourceEx<Deployment>> resourceExes) {
        for (final ResourceEx<Deployment> resourceEx : resourceExes) {
            final Deployment deployment = resourceEx.getResource();
            final DeploymentNode node = new DeploymentNode(this, resourceEx.getSubscriptionId(), deployment);
            addChildNode(node);
        }
    }

    @Override
    public void removeNode(String sid, String id, Node node) {
        EventUtil.executeWithLog(TelemetryConstants.ARM, TelemetryConstants.DELETE_DEPLOYMENT, (operation -> {
            rmNodePresenter.onDeleteDeployment(sid, id);
            removeDirectChildNode(node);
        }), (e) -> {
                DefaultLoader.getUIHelper()
                    .showException("An error occurred while attempting to delete the resource group ",
                        e, "Azure Services Explorer - Error Deleting Resource Group", false, true);
            });
    }

    public String getSid() {
        return sid;
    }

    public String getRgName() {
        return rgName;
    }

    @AzureOperation(name = ActionConstants.ResourceManagement.DELETE, type = AzureOperation.Type.ACTION)
    private void delete() {
        getParent().removeNode(sid, rgName, ResourceManagementNode.this);
    }

}
