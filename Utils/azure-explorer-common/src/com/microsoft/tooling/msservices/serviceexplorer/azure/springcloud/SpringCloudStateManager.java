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

package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.DeploymentResourceInner;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Arrays;

public enum SpringCloudStateManager {
    INSTANCE;

    private final PublishSubject<SpringCloudAppEvent> appSubject = PublishSubject.create();

    public Disposable subscribeSpringAppEvent(@NonNull Consumer<SpringCloudAppEvent> onNext, String... ids) {
        if (ids.length == 0) {
            throw new IllegalArgumentException("Empty ids is illegal.");
        }
        return appSubject.filter(event -> CollectionUtils.containsAny(Arrays.asList(ids), event.getClusterId(),
                                                                      event.getId())).subscribe(onNext);
    }

    public void notifySpringAppUpdate(String clusterId, AppResourceInner appInner, DeploymentResourceInner deploymentInner) {
        DefaultLoader.getIdeHelper().invokeLater(() -> {
            appSubject.onNext(new SpringCloudAppEvent(SpringCloudAppEvent.EventKind.SPRING_APP_UPDATE, clusterId,
                                                      appInner,
                                                      deploymentInner));
        });

    }

    public void notifySpringAppDelete(String clusterId, String appId) {
        DefaultLoader.getIdeHelper().invokeLater(() -> {
            appSubject.onNext(new SpringCloudAppEvent(SpringCloudAppEvent.EventKind.SPRING_APP_DELETE, clusterId, appId));
        });
    }
}
