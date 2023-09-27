/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public interface AzureFormPanel<T> extends AzureForm<T> {
    default void setVisible(boolean visible) {
        // do nothing
    }

    @Override
    @Nullable
    default T getValue() {
        throw new AzureToolkitRuntimeException("method not implemented");
    }

    @Override
    default List<AzureFormInput<?>> getInputs() {
        return new ArrayList<>();
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    default void $$$setupUI$$$() {
    }
}
