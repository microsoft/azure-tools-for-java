/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

public abstract class SparkCluster implements IClusterDetail {
    @Override
    public boolean isEmulator() {
        return false;
    }

    @Override
    public boolean isConfigInfoAvailable() {
        return false;
    }

    @Override
    public ClusterType getType() {
        return ClusterType.spark;
    }

}
