/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.runner.deploy;

import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import rx.Observable;

public class SpringCloudDeploySettingPresenter extends MvpPresenter<SpringCloudDeploySettingMvpView> {
    private static final String CANNOT_LIST_SUBSCRIPTION = "Failed to list subscriptions.";
    private static final String CANNOT_LIST_CLUSTER = "Failed to list clusters.";
    private static final String CANNOT_LIST_APP = "Failed to list apps for cluster: ";

    /**
     * Load subscriptions from model.
     */
    public void onLoadSubscription() {
        Observable.fromCallable(() -> AzureMvpModel.getInstance().getSelectedSubscriptions())
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(subscriptions -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().fillSubscription(subscriptions);
                }));
    }

    public void onLoadClusters(String sid) {
        Observable.fromCallable(() -> AzureSpringCloudMvpModel.listAllSpringCloudClustersBySubscription(sid))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(clusters -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().fillClusters(clusters);
                }));
    }

    public void onLoadApps(String clusterId) {
        Observable.fromCallable(() -> AzureSpringCloudMvpModel.listAppsByClusterId(clusterId))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(apps -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().fillApps(apps);
                }));
    }

    private void errorHandler(String msg, Exception e) {
        DefaultLoader.getIdeHelper().invokeLater(() -> {
            if (isViewDetached()) {
                return;
            }
            getMvpView().onErrorWithException(msg, e);
        });
    }
}
