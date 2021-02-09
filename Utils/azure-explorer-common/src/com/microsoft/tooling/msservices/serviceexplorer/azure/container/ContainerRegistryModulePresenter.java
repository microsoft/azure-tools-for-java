/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.container;

import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.container.ContainerRegistryMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;

import java.util.List;

public class ContainerRegistryModulePresenter<V extends ContainerRegistryModule> extends MvpPresenter<V> {

    /**
     * Called from view when the view needs refresh.
     */
    public void onModuleRefresh() {
        List<ResourceEx<Registry>> registryList = ContainerRegistryMvpModel.getInstance().listContainerRegistries(true);
        if (getMvpView() == null) {
            return;
        }
        registryList.forEach(app -> getMvpView().addChildNode(new ContainerRegistryNode(
                getMvpView(),
                app.getSubscriptionId(),
                app.getResource().id(),
                app.getResource().name()
        )));
    }
}
