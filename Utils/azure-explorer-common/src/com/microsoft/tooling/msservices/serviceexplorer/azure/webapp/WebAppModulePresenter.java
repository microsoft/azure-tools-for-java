/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;

public class WebAppModulePresenter<V extends WebAppModuleView> extends MvpPresenter<V> {
    /**
     * Called from view when the view needs refresh.
     */
    public void onModuleRefresh() {
        final WebAppModuleView view = getMvpView();
        if (view != null) {
            view.renderChildren(AzureWebAppMvpModel.getInstance().listAllWebApps(true));
        }
    }

    public void onDeleteWebApp(String sid, String id) {
        AzureWebAppMvpModel.getInstance().deleteWebApp(sid, id);
    }
}
