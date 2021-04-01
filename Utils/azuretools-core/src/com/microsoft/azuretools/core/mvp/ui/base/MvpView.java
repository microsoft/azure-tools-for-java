/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2018-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.ui.base;

public interface MvpView {

    default void onError(String message) {
        MvpUIHelper uiHelper = MvpUIHelperFactory.getInstance().getMvpUIHelper();
        if (uiHelper == null) {
            return;
        }
        uiHelper.showError(message);
    }

    default void onErrorWithException(String message, Exception ex) {
        MvpUIHelper uiHelper = MvpUIHelperFactory.getInstance().getMvpUIHelper();
        if (uiHelper == null) {
            return;
        }
        throw new RuntimeException(message, ex);
    }
}
