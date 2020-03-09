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
 *
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azure.management.appplatform.v2019_05_01_preview.DeploymentResource;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpringCloudNodePresenter<V extends SpringCloudNodeView> extends MvpPresenter<V> {
    public void onRefreshSpringCloudServiceNode(String subscriptionId, String clusterId) throws IOException {
        final SpringCloudNodeView view = getMvpView();
        if (view != null) {
            AzureSpringCloudMvpModel model = AzureSpringCloudMvpModel.getInstance();
            final List<AppResourceInner> appList = model.listAppsByClusterId(clusterId);
            final Observable<DeploymentResource> deployList = model.listAllDeploymentsByClusterId(clusterId);
            Map<String, DeploymentResource> activeDeployments = new ConcurrentHashMap<>();
            for (DeploymentResource t : deployList.toBlocking().toIterable()) {
                for (AppResourceInner app : appList) {
                    if (StringUtils.equals(app.name(), t.properties().appName()) &&
                        StringUtils.equals(app.properties().activeDeploymentName(), t.name())) {
                        activeDeployments.put(t.properties().appName(), t);
                    }
                }
            }

            view.renderSpringCloudApps(appList, activeDeployments);
        }
    }
}
