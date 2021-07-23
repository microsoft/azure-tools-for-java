/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.arm;

import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
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
        super(resourceGroup.getId(), resourceGroup.getName(), parent, null, true);
        rmNodePresenter = new ResourceManagementNodePresenter();
        rmNodePresenter.onAttachView(this);
        sid = subscriptionId;
        rgName = resourceGroup.getName();
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
        addAction(initActionBuilder(this::openInPortal).withAction(AzureActionEnum.OPEN_IN_PORTAL).withBackgroudable(true).build());
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

    @AzureOperation(name = "arm|rg.delete", params = {"this.rgName"}, type = AzureOperation.Type.ACTION)
    private void delete() {
        getParent().removeNode(sid, rgName, ResourceManagementNode.this);
    }

    @AzureOperation(name = "arm|rg.open_portal", params = {"this.rgName"}, type = AzureOperation.Type.ACTION)
    private void openInPortal() {
        this.openResourcesInPortal(this.sid, this.getId());
    }

}
