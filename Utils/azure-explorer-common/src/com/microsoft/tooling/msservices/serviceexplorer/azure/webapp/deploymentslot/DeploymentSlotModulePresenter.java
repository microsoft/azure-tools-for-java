package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot;

import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import rx.Observable;

public class DeploymentSlotModulePresenter<V extends DeploymentSlotModuleView> extends MvpPresenter<V> {
    private static final String CANNOT_GET_DEPLOYMENT_SLOTS = "Cannot get deployment slots.";

    public void onRefreshDeploymentSlotModule(final String subscriptionId, final String webAppId) {
        Observable.fromCallable(() -> AzureWebAppMvpModel.getInstance().getDeploymentSlots(subscriptionId, webAppId))
            .subscribeOn(getSchedulerProvider().io())
            .subscribe(deploymentSlots -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (isViewDetached()) {
                    return;
                }
                getMvpView().renderDeploymentSlots(deploymentSlots);
                // todo improve the error handling
            }), e -> errorHandler(CANNOT_GET_DEPLOYMENT_SLOTS, (Exception) e));
    }

    private void errorHandler(final String msg, final Exception e) {
        DefaultLoader.getIdeHelper().invokeLater(() -> {
            if (isViewDetached()) {
                return;
            }
            getMvpView().onErrorWithException(msg, e);
        });
    }
}
