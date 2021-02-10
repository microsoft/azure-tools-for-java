/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.jobs.framework;

import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

public class AppRequest implements IRequest {
    private final HttpRequestType restType;
    private final String query;
    private final IClusterDetail clusterDetail;

    // TODO: paser query to a real "query by map"?
    public AppRequest(@NotNull String clusterName, @NotNull String restType, @NotNull String query) {
        this.restType = HttpRequestType.fromString(restType);
        this.query = query;
        this.clusterDetail = JobViewManager.getCluster(clusterName);
    }

    @Override
    public HttpRequestType getRestType() {
        return restType;
    }

    @Nullable
    @Override
    public IClusterDetail getCluster() {
        return clusterDetail;
    }

    // TODO: REST api generated
    @Override
    public String getRequestUrl() {
        switch (restType) {
            case YarnRest:
                return clusterDetail.getConnectionUrl() + "ws/v1/cluster" + query;
            case SparkRest:
                return clusterDetail.getConnectionUrl() + "/sparkhistory/api/v1/" + query;
            default:
                return null;
        }
    }
}
