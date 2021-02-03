/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.deploy.ui;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import rx.Observable;
import rx.Subscription;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.intellij.util.RxJavaUtils.unsubscribeSubscription;

public class FunctionDeployViewPresenter<V extends FunctionDeployMvpView> extends MvpPresenter<V> {

    private Subscription loadAppSettingsSubscription;

    @AzureOperation(
        value = "load app settings of function app[%s]",
        params = {"$functionApp.name()"},
        type = AzureOperation.Type.SERVICE
    )
    public void loadAppSettings(FunctionApp functionApp) {
        if (functionApp == null) {
            return;
        }
        unsubscribeSubscription(loadAppSettingsSubscription);
        loadAppSettingsSubscription = Observable.fromCallable(() -> {
            AzureTaskManager.getInstance().runAndWait(() -> getMvpView().beforeFillAppSettings(), AzureTask.Modality.ANY);
            return functionApp.getAppSettings();
        }).subscribeOn(getSchedulerProvider().io())
                .subscribe(appSettings -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    final Map<String, String> result = new HashMap<>();
                    appSettings.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().value()));
                    getMvpView().fillAppSettings(result);
                }));
    }
}
