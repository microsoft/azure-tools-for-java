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
