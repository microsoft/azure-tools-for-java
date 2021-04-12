/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.jobs;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public class ApplicationKey {
    private final IClusterDetail clusterDetail;
    private final String appId;

    public ApplicationKey(@NotNull IClusterDetail clusterDetail, @NotNull String appId) {
        this.clusterDetail = clusterDetail;
        this.appId = appId;
    }

    public IClusterDetail getClusterDetails() {
        return clusterDetail;
    }

    public String getClusterConnString() {
        return getClusterDetails().getConnectionUrl();
    }

    public String getAppId() {
        return appId;
    }

    @Override
    public int hashCode() {
        return getClusterConnString().toLowerCase().hashCode() + getAppId().toLowerCase().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj instanceof ApplicationKey) {
            ApplicationKey that = (ApplicationKey)obj;
            return getClusterConnString().equalsIgnoreCase(that.getClusterConnString()) &&
                    getAppId().equalsIgnoreCase(that.getClusterConnString());
        }
        return false;
    }
}
