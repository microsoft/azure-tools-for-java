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
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.exception.ExceptionUtils;
import rx.Observable;
import rx.Subscription;

import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.intellij.util.RxJavaUtils.unsubscribeSubscription;

public class FunctionDeployViewPresenter<V extends FunctionDeployMvpView> extends MvpPresenter<V> {

    private static final String CANNOT_LIST_WEB_APP = "Failed to list function apps.";
    private static final String CANNOT_SHOW_APP_SETTINGS = "Failed to show app settings";

    private Subscription loadAppSettingsSubscription;

    public void loadAppSettings(FunctionApp functionApp) {
        if (functionApp == null) {
            return;
        }
        unsubscribeSubscription(loadAppSettingsSubscription);
        loadAppSettingsSubscription = Observable.fromCallable(() -> {
            DefaultLoader.getIdeHelper().invokeAndWait(() -> getMvpView().beforeFillAppSettings());
            return functionApp.getAppSettings();
        }).subscribeOn(getSchedulerProvider().io())
                .subscribe(appSettings -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    final Map<String, String> result = new HashMap<>();
                    appSettings.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().value()));
                    getMvpView().fillAppSettings(result);
                }), e -> errorHandler(CANNOT_SHOW_APP_SETTINGS, (Exception) e));
    }

    private void errorHandler(String msg, Exception e) {
        final Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (rootCause instanceof InterruptedIOException || rootCause instanceof InterruptedException) {
            // Swallow interrupted exception caused by unsubscribe
            return;
        }
        DefaultLoader.getIdeHelper().invokeLater(() -> {
            if (isViewDetached()) {
                return;
            }
            getMvpView().onErrorWithException(msg, e);
        });
    }
}
