/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner.springcloud.deploy;

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
