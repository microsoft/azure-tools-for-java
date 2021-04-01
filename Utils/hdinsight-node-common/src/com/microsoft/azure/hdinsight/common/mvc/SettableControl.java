/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common.mvc;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public interface SettableControl<T> {
    // Data -> Component
    public void setData(@NotNull T data);

    // Component -> Data
    public void getData(@NotNull T data);
}
