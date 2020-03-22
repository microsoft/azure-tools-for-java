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
package com.microsoft.intellij.runner.webapp.webappconfig.ui;

import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import rx.Observable;

public class WebAppDeployViewPresenter<V extends WebAppDeployMvpView> extends MvpPresenter<V> {

    private static final String CANNOT_LIST_WEB_APP = "Failed to list web apps.";
    private static final String CANNOT_LIST_RES_GRP = "Failed to list resource groups.";
    private static final String CANNOT_LIST_APP_SERVICE_PLAN = "Failed to list app service plan.";
    private static final String CANNOT_LIST_SUBSCRIPTION = "Failed to list subscriptions.";
    private static final String CANNOT_LIST_LOCATION = "Failed to list locations.";
    private static final String CANNOT_LIST_PRICING_TIER = "Failed to list pricing tier.";
    private static final String CANNOT_GET_DEPLOYMENT_SLOTS = "Failed to get the deployment slots.";

    public void onRefresh() {
        loadWebApps(true /*forceRefresh*/);
    }

    public void onLoadWebApps() {
        loadWebApps(false /*forceRefresh*/);
    }

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
                }), e -> errorHandler(CANNOT_LIST_SUBSCRIPTION, (Exception) e));
    }

    /**
     * Load resource groups from model.
     */
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

    /**
     * Load app service plan from model.
     */
    public void onLoadAppServicePlan(String sid, String group) {
        Observable.fromCallable(() -> AzureWebAppMvpModel.getInstance().listAppServicePlanBySubscriptionId(sid))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(appServicePlans -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().fillAppServicePlan(appServicePlans);
                }), e -> errorHandler(CANNOT_LIST_APP_SERVICE_PLAN, (Exception) e));
    }

    /**
     * Load locations from model.
     */
    public void onLoadLocation(String sid) {
        Observable.fromCallable(() -> AzureMvpModel.getInstance().listLocationsBySubscriptionId(sid))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(locations -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().fillLocation(locations);
                }), e -> errorHandler(CANNOT_LIST_LOCATION, (Exception) e));
    }

    /**
     * Load pricing tier from model.
     */
    public void onLoadPricingTier() {
        try {
            getMvpView().fillPricingTier(AzureMvpModel.getInstance().listPricingTier());
        } catch (IllegalAccessException e) {
            errorHandler(CANNOT_LIST_PRICING_TIER, e);
        }
    }

    /**
     * Load web containers from model.
     */
    public void onLoadWebContainer() {
        getMvpView().fillWebContainer(AzureWebAppMvpModel.getInstance().listWebContainers());
    }

    /**
     * Load Java versions from model.
     */
    public void onLoadJavaVersions() {
        getMvpView().fillJdkVersion(AzureWebAppMvpModel.getInstance().listJdks());
    }

    /**
     * Load Java Linux runtimes from model.
     */
    public void onLoadLinuxRuntimes() {
        getMvpView().fillLinuxRuntime(AzureWebAppMvpModel.getInstance().getLinuxRuntimes());
    }

    /**
     * Load the deployment slots of the selected web app.
     */
    public void onLoadDeploymentSlots(final String subscriptionId, final String webAppId) {
        Observable.fromCallable(() -> AzureWebAppMvpModel.getInstance().getDeploymentSlots(subscriptionId, webAppId))
            .subscribeOn(getSchedulerProvider().io())
            .subscribe(slots -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (isViewDetached()) {
                    return;
                }
                getMvpView().fillDeploymentSlots(slots);
            }), e -> errorHandler(CANNOT_GET_DEPLOYMENT_SLOTS, (Exception) e));
    }

    private void loadWebApps(boolean forceRefresh) {
        Observable.fromCallable(() -> AzureWebAppMvpModel.getInstance().listAllWebApps(forceRefresh))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(webAppList -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().renderWebAppsTable(webAppList);
                    getMvpView().enableDeploymentSlotPanel();
                }), e -> errorHandler(CANNOT_LIST_WEB_APP, (Exception) e));
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
