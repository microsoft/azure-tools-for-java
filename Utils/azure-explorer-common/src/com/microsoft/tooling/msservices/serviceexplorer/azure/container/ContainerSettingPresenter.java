/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.container;

import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.container.ContainerRegistryMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;

public class ContainerSettingPresenter<V extends ContainerSettingView> extends MvpPresenter<V> {

    private static final String CANNOT_LIST_CONTAINER_REGISTRY = "Cannot list Container Registries.";

    /**
     * Called when UI need to list container registries.
     */
    public void onListRegistries() {
        Observable.fromCallable(() -> {
            List<Registry> registries = new ArrayList<>();
            List<ResourceEx<Registry>> registryList = ContainerRegistryMvpModel.getInstance()
                    .listContainerRegistries(true /*force*/);
            for (ResourceEx<Registry> registry : registryList) {
                if (registry.getResource().adminUserEnabled()) {
                    registries.add(registry.getResource());
                }
            }
            return registries;
        })
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(registries -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().listRegistries(registries);
                }), e -> errorHandler(CANNOT_LIST_CONTAINER_REGISTRY, (Exception) e));
    }

    /**
     * Called when UI need to fill the user credential.
     */
    public void onGetRegistryCredential(@NotNull Registry registry) {
        Observable.fromCallable(() -> ContainerRegistryMvpModel.getInstance().createImageSettingWithRegistry(registry))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(credential -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().fillCredential(credential);
                }), e -> errorHandler(e.getMessage(), (Exception) e));
    }

    private void errorHandler(String msg, Exception e) {
        DefaultLoader.getIdeHelper().invokeLater(() -> {
            if (isViewDetached()) {
                return;
            }
            getMvpView().onErrorWithException(msg, e);
        });
    }
}
