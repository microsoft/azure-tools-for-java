/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentResource;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpringCloudNodePresenter<V extends SpringCloudNodeView> extends MvpPresenter<V> {
    public void onRefreshSpringCloudServiceNode(final String subscriptionId, final String clusterId) {
        final SpringCloudNodeView view = getMvpView();
        if (view != null) {
            final List<AppResourceInner> appList = AzureSpringCloudMvpModel.listAppsByClusterId(clusterId);
            final Observable<DeploymentResource> deployList = AzureSpringCloudMvpModel.listAllDeploymentsByClusterId(clusterId);
            final Map<String, DeploymentResource> activeDeployments = new HashMap<>();
            for (final DeploymentResource deployment : deployList.toBlocking().toIterable()) {
                for (final AppResourceInner app : appList) {
                    if (StringUtils.equals(app.name(), deployment.properties().appName())
                            && StringUtils.equals(app.properties().activeDeploymentName(), deployment.name())) {
                        activeDeployments.put(deployment.properties().appName(), deployment);
                    }
                }
            }

            view.renderSpringCloudApps(appList, activeDeployments);
        }
    }

}
