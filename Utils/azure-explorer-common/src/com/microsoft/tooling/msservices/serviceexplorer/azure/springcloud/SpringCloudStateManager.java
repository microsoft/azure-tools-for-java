/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentResourceInner;
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
