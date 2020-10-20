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

package com.microsoft.intellij.runner.webapp.webappconfig.slimui;

import com.microsoft.azure.toolkit.intellij.webapp.WebAppComboBoxModel;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import rx.Observable;
import rx.Subscription;

import java.io.InterruptedIOException;

import static com.microsoft.intellij.util.RxJavaUtils.unsubscribeSubscription;

public class WebAppDeployViewPresenterSlim<V extends WebAppDeployMvpViewSlim> extends MvpPresenter<V> {

    private static final String CANNOT_LIST_WEB_APP = "Failed to list web apps.";
    private static final String CANNOT_GET_DEPLOYMENT_SLOTS = "Failed to get the deployment slots.";

    private Subscription loadSlotsSubscription;
    private Subscription loadWebAppsSubscription;

    public void onLoadDeploymentSlots(final WebAppComboBoxModel selectedWebApp) {
        if (selectedWebApp == null || StringUtils.isEmpty(selectedWebApp.getResourceId())) {
            return;
        }
        unsubscribeSubscription(loadSlotsSubscription);
        loadSlotsSubscription = Observable.fromCallable(() -> AzureWebAppMvpModel.getInstance().getDeploymentSlots(
                selectedWebApp.getSubscriptionId(), selectedWebApp.getResourceId()))
                  .subscribeOn(getSchedulerProvider().io())
                  .subscribe(slots -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                      if (slots == null || isViewDetached()) {
                          return;
                      }
                      getMvpView().fillDeploymentSlots(slots, selectedWebApp);
                  }), e -> errorHandler(CANNOT_GET_DEPLOYMENT_SLOTS, (Exception) e));
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
