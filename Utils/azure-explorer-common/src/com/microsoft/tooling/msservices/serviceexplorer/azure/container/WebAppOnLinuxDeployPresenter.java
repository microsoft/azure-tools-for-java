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

package com.microsoft.tooling.msservices.serviceexplorer.azure.container;

import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.util.List;
import java.util.stream.Collectors;

import rx.Observable;

public class WebAppOnLinuxDeployPresenter<V extends WebAppOnLinuxDeployView> extends MvpPresenter<V> {
    private static final String CANNOT_LIST_WEB_APP = "Failed to list web apps.";
    private static final String CANNOT_LIST_SUBSCRIPTION = "Failed to list subscriptions.";
    private static final String CANNOT_LIST_RESOURCE_GROUP = "Failed to list resource group.";
    private static final String CANNOT_LIST_PRICING_TIER = "Failed to list pricing tier.";
    private static final String CANNOT_LIST_LOCATION = "Failed to list location.";
    private static final String CANNOT_LIST_APP_SERVICE_PLAN = "Failed to list app service plan.";

    private List<ResourceEx<WebApp>> retrieveListOfWebAppOnLinux(boolean force) {
        return AzureWebAppMvpModel.getInstance().listAllWebAppsOnLinux(force);
    }

    /**
     * Load list of Web App on Linux from cache (if exists).
     */
    public void onLoadAppList() {
        Observable.fromCallable(() -> retrieveListOfWebAppOnLinux(false))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(webAppList -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().renderWebAppOnLinuxList(webAppList);
                }), e -> errorHandler(CANNOT_LIST_WEB_APP, (Exception) e));
    }

    /**
     * Force to refresh list of Web App on Linux.
     */
    public void onRefreshList() {
        Observable.fromCallable(() -> retrieveListOfWebAppOnLinux(true))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(webAppList -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().renderWebAppOnLinuxList(webAppList);
                }), e -> errorHandler(CANNOT_LIST_WEB_APP, (Exception) e));
    }

    /**
     * Load list of Subscriptions.
     */
    public void onLoadSubscriptionList() {
        Observable.fromCallable(() -> AzureMvpModel.getInstance().getSelectedSubscriptions())
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(subscriptions -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().renderSubscriptionList(subscriptions);
                }), e -> errorHandler(CANNOT_LIST_SUBSCRIPTION, (Exception) e));
    }

    private void errorHandler(String msg, Exception e) {
        DefaultLoader.getIdeHelper().invokeLater(() -> {
            if (isViewDetached()) {
                return;
            }
            getMvpView().onErrorWithException(msg, e);
        });
    }

    /**
     * Load List of Resource Group by subscription id.
     * @param sid Subscription Id.
     */
    public void onLoadResourceGroup(String sid) {
        Observable.fromCallable(() -> AzureMvpModel.getInstance().getResourceGroupsBySubscriptionId(sid))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(resourceGroupList -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().renderResourceGroupList(resourceGroupList);
                }), e -> errorHandler(CANNOT_LIST_RESOURCE_GROUP, (Exception) e));
    }

    /**
     * Load List of Location by subscription id.
     * @param sid Subscription Id.
     */
    public void onLoadLocationList(String sid) {
        Observable.fromCallable(() -> AzureMvpModel.getInstance().listLocationsBySubscriptionId(sid))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(locationList -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().renderLocationList(locationList);
                }), e -> errorHandler(CANNOT_LIST_LOCATION, (Exception) e));

    }

    /**
     * Load List of Pricing Tier.
     */
    public void onLoadPricingTierList() {
        Observable.fromCallable(() -> AzureMvpModel.getInstance().listPricingTier())
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(pricingTierList -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().renderPricingTierList(pricingTierList.stream()
                            .filter(item -> !item.equals(PricingTier.FREE_F1) && !item.equals(PricingTier.SHARED_D1))
                            .collect(Collectors.toList()));
                }), e -> errorHandler(CANNOT_LIST_PRICING_TIER, (Exception) e));
    }

    /**
     * Load list of App Service Plan by Subscription and Resource Group.
     * @param sid Subscription Id.
     * @param rg Resource group name.
     */
    public void onLoadAppServicePlan(String sid, String rg) {
        Observable.fromCallable(() -> AzureWebAppMvpModel.getInstance()
                .listAppServicePlanBySubscriptionIdAndResourceGroupName(sid, rg).stream()
                .filter(asp-> OperatingSystem.LINUX.equals(asp.operatingSystem()))
                .collect(Collectors.toList()))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(appServicePlans -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().renderAppServicePlanList(appServicePlans);
                }), e -> errorHandler(CANNOT_LIST_APP_SERVICE_PLAN, (Exception) e));
    }

    /**
     * Load list of App Service Plan by Subscription.
     * TODO: Blocked by SDK, it can only list Windows ASP now.
     * @param sid Subscription Id.
     */
    public void onLoadAppServicePlan(String sid) {
        Observable.fromCallable(() -> AzureWebAppMvpModel.getInstance()
                .listAppServicePlanBySubscriptionId(sid))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(appServicePlans -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().renderAppServicePlanList(appServicePlans);
                }), e -> errorHandler(CANNOT_LIST_APP_SERVICE_PLAN, (Exception) e));
    }
}
