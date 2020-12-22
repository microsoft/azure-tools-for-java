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

package com.microsoft.intellij.runner.functions.deploy.ui;

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
