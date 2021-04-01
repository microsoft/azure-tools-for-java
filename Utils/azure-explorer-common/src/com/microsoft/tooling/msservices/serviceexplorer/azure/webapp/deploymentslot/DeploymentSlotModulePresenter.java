/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2020-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotModulePresenterBase;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotModuleView;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;

public class DeploymentSlotModulePresenter<TView extends DeploymentSlotModuleView<DeploymentSlot>> extends
        DeploymentSlotModulePresenterBase<DeploymentSlot, TView> {

    @Override
    public void onRefreshDeploymentSlotModule(@NotNull String subscriptionId,
                                              @NotNull String appId) {
        final DeploymentSlotModuleView<DeploymentSlot> view = getMvpView();
        if (view != null) {
            view.renderDeploymentSlots(AzureWebAppMvpModel.getInstance().getDeploymentSlots(subscriptionId, appId));
        }
    }

    public void onDeleteDeploymentSlot(@NotNull final String subscriptionId,
                                       @NotNull final String webAppId,
                                       @NotNull final String slotName) {
        AzureWebAppMvpModel.getInstance().deleteDeploymentSlotNode(subscriptionId, webAppId, slotName);
    }
}
