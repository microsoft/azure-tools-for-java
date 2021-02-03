/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule;

import java.util.List;

public class DeploymentSlotModule extends AzureRefreshableNode implements DeploymentSlotModuleView {
    private static final String MODULE_ID = WebAppModule.class.getName();
    private static final String ICON_PATH = "Slot_16.png";

    private final DeploymentSlotModulePresenter presenter;
    protected static final String MODULE_NAME = "Deployment Slots";
    protected final String subscriptionId;
    protected final WebApp webapp;

    public DeploymentSlotModule(final Node parent, final String subscriptionId, final WebApp webapp) {
        super(MODULE_ID, MODULE_NAME, parent, ICON_PATH);
        this.subscriptionId = subscriptionId;
        this.webapp = webapp;
        presenter = new DeploymentSlotModulePresenter<>();
        presenter.onAttachView(this);
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        boolean isLinux = OperatingSystem.LINUX.name().equalsIgnoreCase(webapp.operatingSystem().toString());
        return isLinux ? AzureIconSymbol.DeploymentSlot.MODULE_ON_LINUX : AzureIconSymbol.DeploymentSlot.MODULE;
    }

    @Override
    @AzureOperation(value = "remove deployment slot", type = AzureOperation.Type.ACTION)
    public void removeNode(final String sid, final String name, Node node) {
        presenter.onDeleteDeploymentSlot(sid, this.webapp.id(), name);
        removeDirectChildNode(node);
    }

    @Override
    @AzureOperation(value = "refresh deployment slots", type = AzureOperation.Type.ACTION)
    protected void refreshItems() {
        presenter.onRefreshDeploymentSlotModule(this.subscriptionId, this.webapp.id());
    }

    @Override
    public void renderDeploymentSlots(@NotNull final List<DeploymentSlot> slots) {
        slots.forEach(slot -> addChildNode(
            new DeploymentSlotNode(slot.id(), slot.parent().id(), slot.parent().name(),
                                   this, slot.name(), slot.state(), slot.operatingSystem().toString(),
                                   this.subscriptionId, slot.defaultHostName())));
    }
}
