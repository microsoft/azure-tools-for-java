/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.ui.webapp.deploysetting;

import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import rx.Observable;


public class WebAppDeployViewPresenter<V extends WebAppDeployMvpView> extends MvpPresenter<V> {

    private static final String CANNOT_LIST_WEB_APP = "Failed to list web apps.";
    private static final String CANNOT_LIST_RES_GRP = "Failed to list resource groups.";
    private static final String CANNOT_LIST_APP_SERVICE_PLAN = "Failed to lsit app service plan.";
    private static final String CANNOT_LIST_SUBSCRIPTION = "Failed to list subscriptions.";

    public void onRefresh(boolean forceRefresh) {
        Observable.fromCallable(() -> AzureWebAppMvpModel.getInstance().listJavaWebApps(forceRefresh))
        .subscribeOn(getSchedulerProvider().io())
        .subscribe(webAppList -> DefaultLoader.getIdeHelper().invokeLater(() -> {
            if (isViewDetached()) {
                return;
            }
            getMvpView().renderWebAppsTable(webAppList);
        }), e -> errorHandler(CANNOT_LIST_WEB_APP, (Exception) e));
    }

    public void onLoadSubscription() {
        Observable.fromCallable(() -> AzureMvpModel.getInstance().getSelectedSubscriptions())
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(subscriptions -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().fillSubscription(subscriptions);
        }), e -> errorHandler(CANNOT_LIST_SUBSCRIPTION, (Exception) e));
    }

    public void onLoadResourceGroups(String sid) {
        Observable.fromCallable(() -> AzureMvpModel.getInstance().getResourceGroupsBySubscriptionId(sid))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(resourceGroups -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().fillResourceGroup(resourceGroups);
                }), e -> errorHandler(CANNOT_LIST_RES_GRP, (Exception) e));
    }

    public void onLoadAppServicePlan(String sid, String group) {
        Observable.fromCallable(() -> AzureWebAppMvpModel.getInstance().listAppServicePlanBySubscriptionIdAndResrouceGroupName(sid, group))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(appServicePlans -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().fillAppServicePlan(appServicePlans);
                }), e -> errorHandler(CANNOT_LIST_APP_SERVICE_PLAN, (Exception) e));
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
