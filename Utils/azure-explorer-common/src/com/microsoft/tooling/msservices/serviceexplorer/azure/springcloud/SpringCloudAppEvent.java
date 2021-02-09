/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentResourceInner;

public class SpringCloudAppEvent {
    enum EventKind {
        SPRING_APP_UPDATE, SPRING_APP_DELETE
    }
    private EventKind kind;
    private String id;
    private String clusterId;
    private AppResourceInner appInner;
    private DeploymentResourceInner deploymentInner;

    public SpringCloudAppEvent(EventKind kind, String clusterId, AppResourceInner appInner, DeploymentResourceInner deploymentInner) {
        this.kind = kind;
        this.clusterId = clusterId;
        this.appInner = appInner;
        this.id = appInner == null ? null : appInner.id();
        this.deploymentInner = deploymentInner;
    }

    public SpringCloudAppEvent(EventKind kind, String clusterId, String id) {
        this.kind = kind;
        this.clusterId = clusterId;
        this.id = id;
    }

    public EventKind getKind() {
        return kind;
    }

    public String getId() {
        return id;
    }

    public String getClusterId() {
        return clusterId;
    }

    public AppResourceInner getAppInner() {
        return appInner;
    }

    public DeploymentResourceInner getDeploymentInner() {
        return deploymentInner;
    }

    public boolean isDelete() {
        return kind == EventKind.SPRING_APP_DELETE;
    }

    public boolean isUpdate() {
        return kind == EventKind.SPRING_APP_UPDATE;
    }
}
