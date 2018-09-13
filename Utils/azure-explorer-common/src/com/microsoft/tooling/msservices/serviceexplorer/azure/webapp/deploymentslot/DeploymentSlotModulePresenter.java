package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import java.io.IOException;
import java.util.List;

public class DeploymentSlotModulePresenter<V extends DeploymentSlotModule> extends MvpPresenter<V> {
    public void onModuleRefresh(final String subscriptionId, final String webAppId) throws IOException {
        final List<DeploymentSlot> slots = AzureWebAppMvpModel
            .getInstance()
            .getDeploymentSlots(subscriptionId, webAppId);

        slots.forEach(slot -> getMvpView().addChildNode(
            new DeploymentSlotNode(getMvpView(), slot.name(), slot.state(), subscriptionId)
        ));
    }
}
