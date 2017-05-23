package com.microsoft.azure.hdinsight.spark.jobs.framework;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

/**
 * Rest API request object
 *
 */
public interface IRequest {

    @NotNull
    String getRequestUrl();

    @NotNull
    IClusterDetail getCluster();

    @NotNull
    RequestTypeEnum getRestType();
}
