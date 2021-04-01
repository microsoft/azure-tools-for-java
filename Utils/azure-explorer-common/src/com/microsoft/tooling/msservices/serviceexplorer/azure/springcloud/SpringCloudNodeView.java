/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentResource;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azuretools.core.mvp.ui.base.MvpView;

import java.util.List;
import java.util.Map;

public interface SpringCloudNodeView extends MvpView {
    void renderSpringCloudApps(List<AppResourceInner> apps, Map<String, DeploymentResource> map);

    Object getProjectObject();
}
