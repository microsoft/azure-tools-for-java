/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig.slimui;

import com.microsoft.azure.toolkit.intellij.webapp.WebAppComboBoxModel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;
import rx.Subscription;

import static com.microsoft.intellij.util.RxJavaUtils.unsubscribeSubscription;

public class WebAppDeployViewPresenterSlim<V extends WebAppDeployMvpViewSlim> extends MvpPresenter<V> {
    private Subscription loadSlotsSubscription;
    private Subscription loadWebAppsSubscription;

    public void onLoadDeploymentSlots(final WebAppComboBoxModel selectedWebApp) {
        if (selectedWebApp == null || StringUtils.isEmpty(selectedWebApp.getResourceId())) {
            return;
        }
        unsubscribeSubscription(loadSlotsSubscription);
        loadSlotsSubscription = Observable.fromCallable(() -> Azure.az(AzureAppService.class).webapp(selectedWebApp.getResourceId()).deploymentSlots(true))
                  .subscribeOn(getSchedulerProvider().io())
                  .subscribe(slots -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                      if (slots == null || isViewDetached()) {
                          return;
                      }
                      getMvpView().fillDeploymentSlots(slots, selectedWebApp);
                  }));
    }
}
