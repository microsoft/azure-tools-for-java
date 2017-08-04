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

package com.microsoft.intellij.runner.container.webapponlinux.ui;

import com.microsoft.azure.management.appservice.PricingTier;
import rx.Observable;

import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WebAppOnLinuxDeployPresenter<V extends WebAppOnLinuxDeployView> extends MvpPresenter<V> {
    private static final String CANNOT_LIST_WEB_APP = "Failed to list web apps.";
    private static final String CANNOT_LIST_SUBSCRIPTION = "Failed to list subscriptions.";
    private static final String CANNOT_LIST_RESOURCE_GROUP = "Failed to list resource group.";
    private static final String CANNOT_LIST_PRICING_TIER = "Failed to list pricing tier.";
    private static final String CANNOT_LIST_LOCATION = "Failed to list location.";

    private List<ResourceEx<SiteInner>> retrieveListOfWebAppOnLinux(boolean force) {
        List<ResourceEx<SiteInner>> ret = new ArrayList<>();
        for (Subscription sb : AzureMvpModel.getInstance().getSelectedSubscriptions()) {
            List<ResourceEx<SiteInner>> wal = AzureWebAppMvpModel.getInstance()
                    .listWebAppsOnLinuxBySubscriptionId(sb.subscriptionId(), force);
            ret.addAll(wal);
        }
        return ret;
    }

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
}
