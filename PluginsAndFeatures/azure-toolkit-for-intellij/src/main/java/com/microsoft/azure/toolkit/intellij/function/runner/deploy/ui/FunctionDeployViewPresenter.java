/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.deploy.ui;

import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import rx.Observable;
import rx.Subscription;

import static com.microsoft.intellij.util.RxJavaUtils.unsubscribeSubscription;

public class FunctionDeployViewPresenter<V extends FunctionDeployMvpView> extends MvpPresenter<V> {

    private Subscription loadAppSettingsSubscription;

    @AzureOperation(
        name = "function.load_setting",
        params = {"functionApp.name()"},
        type = AzureOperation.Type.SERVICE
    )
    public void loadAppSettings(IFunctionApp functionApp) {
        if (functionApp == null) {
            return;
        }
        unsubscribeSubscription(loadAppSettingsSubscription);
        loadAppSettingsSubscription =
            Observable.fromCallable(() -> functionApp.entity().getAppSettings()).subscribeOn(getSchedulerProvider().io())
                      .subscribe(appSettings -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                          if (isViewDetached()) {
                              return;
                          }
                          getMvpView().fillAppSettings(appSettings);
                      }));
    }
}
