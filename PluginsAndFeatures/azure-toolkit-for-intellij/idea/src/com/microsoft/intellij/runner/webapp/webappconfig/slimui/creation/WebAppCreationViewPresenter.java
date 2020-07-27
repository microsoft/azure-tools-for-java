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

package com.microsoft.intellij.runner.webapp.webappconfig.slimui.creation;

import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.JdkModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import rx.Observable;

public class WebAppCreationViewPresenter<V extends WebAppCreationMvpView> extends MvpPresenter<V> {

    private static final String CANNOT_LIST_RES_GRP = "Failed to list resource groups.";
    private static final String CANNOT_LIST_APP_SERVICE_PLAN = "Failed to list app service plan.";
    private static final String CANNOT_LIST_SUBSCRIPTION = "Failed to list subscriptions.";
    private static final String CANNOT_LIST_LOCATION = "Failed to list locations.";
    private static final String CANNOT_LIST_PRICING_TIER = "Failed to list pricing tier.";

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
    public void onLoadAppServicePlan(String sid) {
        Observable.fromCallable(() -> AzureWebAppMvpModel.getInstance().listAppServicePlanBySubscriptionId(sid))
            .subscribeOn(getSchedulerProvider().io())
            .subscribe(appServicePlans -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (isViewDetached()) {
                    return;
                }
                getMvpView().fillAppServicePlan(appServicePlans);
            }), e -> errorHandler(CANNOT_LIST_APP_SERVICE_PLAN, (Exception) e));
    }

    public void onLoadRegion(String sid, PricingTier pricingTier) {
        Observable.fromCallable(() -> AzureWebAppMvpModel.getInstance().getAvailableRegions(sid, pricingTier))
                  .subscribeOn(getSchedulerProvider().io())
                  .subscribe(regions -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                      if (isViewDetached()) {
                          return;
                      }
                      getMvpView().fillRegion(regions);
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
     * Load war web containers from model.
     */
    public void onLoadWarWebContainer() {
        getMvpView().fillWebContainer(AzureWebAppMvpModel.listWebContainersForWarFile());
    }

    /**
     * Load jar web containers from model.
     */
    public void onLoadJarWebContainer(JdkModel jdkModel) {
        getMvpView().fillWebContainer(AzureWebAppMvpModel.listWebContainersForJarFile(jdkModel));
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

    private void errorHandler(String msg, Exception e) {
        DefaultLoader.getIdeHelper().invokeLater(() -> {
            if (isViewDetached()) {
                return;
            }
            getMvpView().onErrorWithException(msg, e);
        });
    }
}
