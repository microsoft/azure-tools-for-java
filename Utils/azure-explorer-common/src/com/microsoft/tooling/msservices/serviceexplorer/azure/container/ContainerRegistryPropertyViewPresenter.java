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
import com.microsoft.azure.management.containerregistry.RegistryPassword;
import com.microsoft.azure.management.containerregistry.implementation.RegistryListCredentials;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.model.container.ContainerRegistryMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.core.mvp.ui.containerregistry.ContainerRegistryProperty;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.util.List;

import rx.Observable;

public class ContainerRegistryPropertyViewPresenter<V extends ContainerRegistryPropertyMvpView>
        extends MvpPresenter<V> {

    private static final String CANNOT_GET_SUBSCRIPTION_ID = "Cannot get Subscription ID.";
    private static final String CANNOT_GET_REGISTRY_ID = "Cannot get Container Registry's ID.";
    private static final String CANNOT_GET_REGISTRY_PROPERTY = "Cannot get Container Registry's property.";

    /**
     * Called by view when the view needs to load the property of an ACR.
     */
    public void onGetRegistryProperty(String sid, String id) {
        if (Utils.isEmptyString(sid)) {
            getMvpView().onError(CANNOT_GET_SUBSCRIPTION_ID);
            return;
        }
        if (Utils.isEmptyString(id)) {
            getMvpView().onError(CANNOT_GET_REGISTRY_ID);
            return;
        }
        Observable.fromCallable(() -> ContainerRegistryMvpModel.getInstance().getContainerRegistry(sid, id))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(registry -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    if (registry == null) {
                        getMvpView().onError(CANNOT_GET_REGISTRY_PROPERTY);
                        return;
                    }
                    ContainerRegistryProperty property = getProperty(registry, sid);
                    getMvpView().showProperty(property);
                }), e -> errorHandler(CANNOT_GET_REGISTRY_PROPERTY, (Exception) e));
    }

    public void onEnableAdminUser(String sid, String id, boolean b) {
        if (Utils.isEmptyString(sid)) {
            getMvpView().onError(CANNOT_GET_SUBSCRIPTION_ID);
            return;
        }
        if (Utils.isEmptyString(id)) {
            getMvpView().onError(CANNOT_GET_REGISTRY_ID);
            return;
        }
        Observable.fromCallable(() -> ContainerRegistryMvpModel.getInstance().setAdminUserEnabled(sid, id, b))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(registry -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    if (registry == null) {
                        getMvpView().onError(CANNOT_GET_REGISTRY_PROPERTY);
                        return;
                    }
                    ContainerRegistryProperty property = getProperty(registry, sid);
                    getMvpView().showProperty(property);
                }), e -> errorHandler(CANNOT_GET_REGISTRY_PROPERTY, (Exception) e));
    }

    private ContainerRegistryProperty getProperty(Registry registry, String sid) {
        String userName = "";
        String password = "";
        String password2 = "";
        if (registry.adminUserEnabled()) {
            RegistryListCredentials credentials = registry.listCredentials();
            userName = credentials.username();
            List<RegistryPassword> passwords = credentials.passwords();
            if (passwords.size() > 0) {
                password = passwords.get(0).value();
            }
            if (passwords.size() > 1) {
                password2 = passwords.get(1).value();
            }
        }
        return new ContainerRegistryProperty(registry.id(), registry.name(), registry.type(),
                registry.resourceGroupName(), registry.regionName(), sid, registry.loginServerUrl(),
                registry.adminUserEnabled(), userName, password, password2);
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
