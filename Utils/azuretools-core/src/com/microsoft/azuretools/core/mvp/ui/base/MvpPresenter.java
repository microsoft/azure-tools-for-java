/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.ui.base;

public abstract class MvpPresenter<V extends MvpView> {

    private V mvpView;

    public V getMvpView() {
        return mvpView;
    }

    public void onAttachView(V mvpView) {
        this.mvpView = mvpView;
    }

    public void onDetachView() {
        this.mvpView = null;
    }

    public boolean isViewDetached() {
        return this.mvpView == null;
    }

    public SchedulerProvider getSchedulerProvider() {
        return SchedulerProviderFactory.getInstance().getSchedulerProvider();
    }
}
