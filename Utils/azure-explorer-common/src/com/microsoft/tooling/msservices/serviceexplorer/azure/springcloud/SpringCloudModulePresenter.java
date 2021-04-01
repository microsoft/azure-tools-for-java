/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;

public class SpringCloudModulePresenter<V extends SpringCloudModuleView> extends MvpPresenter<V> {
    private static final String FAILED_TO_LOAD_CLUSTERS = "Failed to load Spring Cloud Clusters.";
    private static final String ERROR_LOAD_CLUSTER = "Azure Services Explorer - Error Loading Spring Cloud Clusters";

    public void onSpringCloudRefresh() {
        final SpringCloudModuleView view = getMvpView();
        if (view != null) {
            view.renderChildren(AzureSpringCloudMvpModel.listAllSpringCloudClusters());
        }
    }

}
