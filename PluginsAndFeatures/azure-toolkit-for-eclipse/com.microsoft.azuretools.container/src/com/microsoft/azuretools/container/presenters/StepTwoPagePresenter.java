/**
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

package com.microsoft.azuretools.container.presenters;

import com.microsoft.azuretools.azurecommons.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.container.ConsoleLogger;
import com.microsoft.azuretools.container.ui.wizard.publish.StepTwoPage;
import com.microsoft.azuretools.container.utils.WebAppOnLinuxUtil;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import rx.Observable;
import rx.schedulers.Schedulers;

public class StepTwoPagePresenter<V extends StepTwoPage> extends MvpPresenter<V> {

    public void onLoadWebAppsOnLinux() {
        getMvpView().disablePageOnLoading();
        Observable.fromCallable(() -> {
            AzureModelController.updateSubscriptionMaps(null);
            return WebAppOnLinuxUtil.listAllWebAppOnLinux();
        })
        .subscribeOn(Schedulers.io())
        .subscribe(wal -> {
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                getMvpView().finishLoading(wal);
            });
        }, e -> {
            ConsoleLogger.error("onLoadWebAppsOnLinux@StepTwoPagePresenter");
            getMvpView().onErrorWithException("onLoadWebAppsOnLinux@StepTwoPagePresenter", (Exception) e);
        });
    }
}
