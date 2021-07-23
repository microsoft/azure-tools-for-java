/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.helpers;

import com.microsoft.azuretools.core.mvp.ui.base.MvpUIHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;

public class MvpUIHelperImpl implements MvpUIHelper {

    @Override
    public void showError(String msg) {
        DefaultLoader.getUIHelper().showError(msg, msg);
    }

    @Override
    public void showException(String msg, Exception e) {
        DefaultLoader.getUIHelper().showError(e.getMessage(), msg);
    }
}
