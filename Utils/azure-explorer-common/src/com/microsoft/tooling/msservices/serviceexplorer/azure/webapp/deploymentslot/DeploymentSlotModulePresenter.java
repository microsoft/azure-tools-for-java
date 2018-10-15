package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot;

import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import java.io.IOException;

public class DeploymentSlotModulePresenter<V extends DeploymentSlotModuleView> extends MvpPresenter<V> {
    public void onRefreshDeploymentSlotModule(final String subscriptionId, final String webAppId) throws IOException {
        final DeploymentSlotModuleView view = getMvpView();
        if (view != null) {
            view.renderDeploymentSlots(AzureWebAppMvpModel.getInstance().getDeploymentSlots(subscriptionId, webAppId));
        }
    }

    public void onDeleteDeploymentSlot(final String subscriptionId, final String webAppId,
                                       final String slotName) throws IOException {
        AzureWebAppMvpModel.getInstance().deleteDeploymentSlotNode(subscriptionId, webAppId, slotName);
    }
}
