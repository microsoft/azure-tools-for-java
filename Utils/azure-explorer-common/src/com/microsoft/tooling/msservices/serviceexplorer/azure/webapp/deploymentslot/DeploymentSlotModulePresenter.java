/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot;

import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;

public class DeploymentSlotModulePresenter<V extends DeploymentSlotModuleView> extends MvpPresenter<V> {
    public void onRefreshDeploymentSlotModule(final String subscriptionId, final String webAppId) {
        final DeploymentSlotModuleView view = getMvpView();
        if (view != null) {
            view.renderDeploymentSlots(AzureWebAppMvpModel.getInstance().getDeploymentSlots(subscriptionId, webAppId));
        }
    }

    public void onDeleteDeploymentSlot(final String subscriptionId, final String webAppId,
                                       final String slotName) {
        AzureWebAppMvpModel.getInstance().deleteDeploymentSlotNode(subscriptionId, webAppId, slotName);
    }
}
