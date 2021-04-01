/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2020-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotNodeBase;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotNodePresenterBase;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotNodeView;
import org.jetbrains.annotations.NotNull;

public class DeploymentSlotNode extends DeploymentSlotNodeBase<DeploymentSlot> implements DeploymentSlotNodeView {
    private static final String ACTION_SWAP_WITH_PRODUCTION = "Swap with production";
    private static final String LABEL = "Slot";

    private final DeploymentSlotNodePresenterBase<DeploymentSlot, DeploymentSlotNodeView> myPresenter;

    public DeploymentSlotNode(final String slotId, final String webAppId, final String webAppName,
                              final DeploymentSlotModule parent, final String name, final String state, final String os,
                              final String subscriptionId, final String hostName) {
        super(webAppId, webAppName, parent, slotId, name, state, os, subscriptionId, hostName);
        this.myPresenter = new DeploymentSlotNodePresenter<>();
        this.myPresenter.onAttachView(this);
    }

    @NotNull
    @Override
    public DeploymentSlotNodePresenterBase<DeploymentSlot, DeploymentSlotNodeView> getPresenter() {
        return myPresenter;
    }

    @Override
    public void openDeploymentSlotPropertyAction() {
        DefaultLoader.getUIHelper().openDeploymentSlotPropertyView(this);
    }
}
