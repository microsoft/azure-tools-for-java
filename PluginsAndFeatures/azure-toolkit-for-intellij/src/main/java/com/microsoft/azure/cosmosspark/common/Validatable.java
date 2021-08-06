/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosspark.common;

import com.microsoft.azuretools.azurecommons.helpers.Nullable;

public interface Validatable {
    boolean isLegal();

    @Nullable
    String getErrorMessage();
}
