/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.arm;

import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.utils.CanceledByUserException;

public class ResourceManagementModulePresenter<V extends ResourceManagementModuleView> extends MvpPresenter<V> {

    public void onModuleRefresh() throws CanceledByUserException {
        final ResourceManagementModuleView view = getMvpView();
        if (view != null) {
            view.renderChildren(AzureMvpModel.getInstance().getResourceGroups(true));
        }
    }

    public void onDeleteResourceGroup(String sid, String rgName) {
        Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        azure.resourceGroups().deleteByName(rgName);
    }
}
