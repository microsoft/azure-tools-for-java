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
