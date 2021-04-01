/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.microsoft.azure.toolkit.lib.common.form.AzureForm;

public interface AzureFormPanel<T> extends AzureForm<T> {
    void setVisible(boolean visible);

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    default void $$$setupUI$$$() {
    }
}
