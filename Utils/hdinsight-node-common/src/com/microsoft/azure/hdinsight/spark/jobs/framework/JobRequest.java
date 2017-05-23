package com.microsoft.azure.hdinsight.spark.jobs.framework;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;

public class JobRequest implements IRequest {
    @Override
    public String getRequestUrl() {
        return null;
    }

    @Override
    public IClusterDetail getCluster() {
        return null;
    }

    @Override
    public RequestTypeEnum getRestType() {
        return null;
    }
}
