/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.arm;

import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;

public class ResourceManagementNodePresenter<V extends ResourceManagementNodeView> extends MvpPresenter<V> {

    public void onModuleRefresh(String sid, String rgName) {
        final ResourceManagementNodeView view = getMvpView();
        if (view != null) {
            view.renderChildren(AzureMvpModel.getInstance().getDeploymentByRgName(sid, rgName));
        }
    }

    public void onDeleteDeployment(String sid, String deploymentId) {
        final Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        azure.deployments().deleteById(deploymentId);
    }
}
