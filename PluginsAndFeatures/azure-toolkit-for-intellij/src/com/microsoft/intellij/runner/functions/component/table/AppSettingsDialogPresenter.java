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

package com.microsoft.intellij.runner.functions.component.table;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import rx.Observable;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class AppSettingsDialogPresenter<V extends ImportAppSettingsView> extends MvpPresenter<V> {
    public void onLoadFunctionApps() {
        Observable.fromCallable(() -> AzureFunctionMvpModel.getInstance().listAllFunctions(false))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(functionApps -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().fillFunctionApps(functionApps);
                }), this::errorHandler);
    }

    public void onLoadFunctionAppSettings(String subscriptionId, String functionId) {
        Observable.fromCallable(() -> {
            getMvpView().prepareFillAppSettings();
            final FunctionApp functionApp = AzureFunctionMvpModel.getInstance().getFunctionById(subscriptionId, functionId);
            return functionApp.getAppSettings();
        }).subscribeOn(getSchedulerProvider().io())
                .subscribe(appSettings -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    final Map<String, String> result = new HashMap<>();
                    appSettings.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().value()));
                    getMvpView().fillFunctionAppSettings(result);
                }), this::errorHandler);
    }

    public void onLoadLocalSettings(Path localSettingsJsonPath) {
        Observable.fromCallable(() -> {
            getMvpView().prepareFillAppSettings();
            return AppSettingsTableUtils.getAppSettingsFromLocalSettingsJson(localSettingsJsonPath.toFile());
        }).subscribeOn(getSchedulerProvider().io())
                .subscribe(appSettings -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().fillFunctionAppSettings(appSettings);
                }), this::errorHandler);
    }

    private void errorHandler(Throwable e) {
        DefaultLoader.getIdeHelper().invokeLater(() -> getMvpView().onErrorWithException("Failed to load app settings", (Exception) e));
    }
}
