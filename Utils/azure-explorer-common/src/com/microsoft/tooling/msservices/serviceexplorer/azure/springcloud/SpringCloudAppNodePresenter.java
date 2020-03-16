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

public class SpringCloudAppNodePresenter<V extends SpringCloudAppNodeView> extends MvpPresenter<V> {

    public void onStartSpringCloudApp(String appId, String activeDeploymentName) {
        AzureSpringCloudMvpModel.getInstance().startApp(appId, activeDeploymentName).await();
        updateSelfStatus(appId, activeDeploymentName);
    }

    public void onStopSpringCloudApp(String appId, String activeDeploymentName) {
        AzureSpringCloudMvpModel.getInstance().stopApp(appId, activeDeploymentName).await();
        updateSelfStatus(appId, activeDeploymentName);
    }

    public void onReStartSpringCloudApp(String appId, String activeDeploymentName) {
        AzureSpringCloudMvpModel.getInstance().restartApp(appId, activeDeploymentName).await();
        updateSelfStatus(appId, activeDeploymentName);
    }

    public void onDeleteSpringCloudApp(String appId) {
        AzureSpringCloudMvpModel.getInstance().deleteApp(appId).await();
    }

    private void updateSelfStatus(String appId, String activeDeploymentName) {
        // todo: update status
    }
}
