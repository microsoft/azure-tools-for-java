/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import com.google.common.collect.ImmutableSortedSet;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public interface ClusterContainer {
    @NotNull
    ImmutableSortedSet<? extends IClusterDetail> getClusters();

    @NotNull
    ClusterContainer refresh();
}
