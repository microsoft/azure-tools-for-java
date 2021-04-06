/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2020-2021 JetBrains s.r.o.
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
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotModuleBase;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotModulePresenterBase;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotModuleView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DeploymentSlotModule extends DeploymentSlotModuleBase<DeploymentSlot> implements
        DeploymentSlotModuleView<DeploymentSlot> {

    private static final Logger myLogger = LoggerFactory.getLogger(DeploymentSlotModule.class);

    private static final String MODULE_ID = WebAppModule.class.getName();
    private static final String ICON_PATH = "Slot_16.png";

    private final DeploymentSlotModulePresenterBase<DeploymentSlot, DeploymentSlotModuleView<DeploymentSlot>> myPresenter;

    public static final String MODULE_NAME = "Deployment Slots";

    public DeploymentSlotModule(final Node parent, final String subscriptionId, final WebApp webapp, final boolean isDeploymentSlotsSupported) {
        super(MODULE_ID, parent, subscriptionId, webapp, isDeploymentSlotsSupported, myLogger);
        myPresenter = new DeploymentSlotModulePresenter<>();
        myPresenter.onAttachView(this);
    }

    @NotNull
    @Override
    public DeploymentSlotModulePresenterBase<DeploymentSlot, DeploymentSlotModuleView<DeploymentSlot>> getPresenter() {
        return myPresenter;
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        boolean isLinux = OperatingSystem.LINUX.name().equalsIgnoreCase(getApp().operatingSystem().toString());
        return isLinux ? AzureIconSymbol.DeploymentSlot.MODULE_ON_LINUX : AzureIconSymbol.DeploymentSlot.MODULE;
    }

    @Override
    @AzureOperation(name = "webapp|deployment.delete", params = {"$name", "@webapp.name()"}, type = AzureOperation.Type.ACTION)
    public void removeNode(final String sid, final String name, Node node) {
        myPresenter.onDeleteDeploymentSlot(sid, this.getApp().id(), name);
        removeDirectChildNode(node);
    }

    @Override
    @AzureOperation(name = "webapp|deployment.reload", params = {"@webapp.name()"}, type = AzureOperation.Type.ACTION)
    protected void refreshItems() {
        myPresenter.onRefreshDeploymentSlotModule(this.getSubscriptionId(), this.getApp().id());
    }

    @Override
    public void renderDeploymentSlots(@org.jetbrains.annotations.NotNull List<? extends DeploymentSlot> deploymentSlots) {
        deploymentSlots.forEach(slot -> addChildNode(
                new DeploymentSlotNode(slot.id(), slot.parent().id(), slot.parent().name(),
                        this, slot.name(), slot.state(), slot.operatingSystem().toString(),
                        this.getSubscriptionId(), slot.defaultHostName())));
    }

    @Override
    public int getPriority() {
        return HIGH_PRIORITY;
    }
}
