/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import com.microsoft.azuretools.azurecommons.helpers.Nullable;

public interface YarnCluster {
    String getYarnNMConnectionUrl();

    @Nullable
    default String getYarnUIUrl() {
        return null;
    }
}
