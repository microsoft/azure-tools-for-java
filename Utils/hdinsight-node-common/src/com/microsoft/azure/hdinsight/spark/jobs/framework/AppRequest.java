/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
