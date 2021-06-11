/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.container;

import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import rx.Observable;

import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

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
                }));
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
                }));
    }

    /**
     * Load list of Subscriptions.
     */
    public void onLoadSubscriptionList() {
        Observable.fromCallable(() -> az(AzureAccount.class).account().getSelectedSubscriptions())
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(subscriptions -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().renderSubscriptionList(subscriptions);
                }));
    }

    /**
     * Load List of Resource Group by subscription id.
     *
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
                }));
    }

    /**
     * Load List of Location by subscription id.
     *
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
                }));

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
                }));
    }

    /**
     * Load list of App Service Plan by Subscription and Resource Group.
     *
     * @param sid Subscription Id.
     * @param rg  Resource group name.
     */
    public void onLoadAppServicePlan(String sid, String rg) {
        Mono.fromCallable(() -> Azure.az(AzureAppService.class)
            .subscription(sid).appServicePlansByResourceGroup(rg)).flatMapMany(Flux::fromIterable)
                .filter(asp -> OperatingSystem.LINUX.equals(asp.entity().getOperatingSystem()))
                .subscribeOn(Schedulers.boundedElastic())
                .collectList().subscribe(appServicePlans -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().renderAppServicePlanList(appServicePlans);
                }));
    }

    /**
     * Load list of App Service Plan by Subscription.
     * TODO: Blocked by SDK, it can only list Windows ASP now.
     *
     * @param sid Subscription Id.
     */
    public void onLoadAppServicePlan(String sid) {
        Mono.fromCallable(() -> Azure.az(AzureAppService.class)
            .subscription(sid).appServicePlans()).flatMapMany(Flux::fromIterable)
            .filter(asp -> OperatingSystem.LINUX.equals(asp.entity().getOperatingSystem()))
            .subscribeOn(Schedulers.boundedElastic())
            .collectList().subscribe(appServicePlans -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (isViewDetached()) {
                    return;
                }
                getMvpView().renderAppServicePlanList(appServicePlans);
            }));
    }
}
