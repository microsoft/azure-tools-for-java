/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.ui.base;

public interface MvpUIHelper {

    void showError(String message);

    void showException(String message, Exception e);
}
