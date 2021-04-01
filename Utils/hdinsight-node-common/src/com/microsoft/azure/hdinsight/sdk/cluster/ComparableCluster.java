/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public interface ComparableCluster extends Comparable<ComparableCluster> {
    @NotNull
    String getClusterIdForConfiguration();

    @Override
    default int compareTo(@NotNull ComparableCluster other) {
        if (this == other) {
            return 0;
        }

        return this.getClusterIdForConfiguration().compareToIgnoreCase(other.getClusterIdForConfiguration());
    }
}
