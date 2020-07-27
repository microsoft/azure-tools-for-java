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

import com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.io.IOException;

public class SpringCloudModulePresenter<V extends SpringCloudModuleView> extends MvpPresenter<V> {
    private static final String FAILED_TO_LOAD_CLUSTERS = "Failed to load Spring Cloud Clusters.";
    private static final String ERROR_LOAD_CLUSTER = "Azure Services Explorer - Error Loading Spring Cloud Clusters";

    public void onSpringCloudRefresh() {
        final SpringCloudModuleView view = getMvpView();
        if (view != null) {
            try {
                view.renderChildren(AzureSpringCloudMvpModel.listAllSpringCloudClusters());
            } catch (final IOException e) {
                DefaultLoader.getUIHelper().showException(FAILED_TO_LOAD_CLUSTERS, e, ERROR_LOAD_CLUSTER, false, true);
            }
        }
    }

}
