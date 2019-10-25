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

package com.microsoft.azure.projectarcadia.common;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.ComparableCluster;
import com.microsoft.azure.hdinsight.sdk.cluster.SparkCluster;
import com.microsoft.azure.hdinsight.sdk.rest.azure.projectarcadia.models.SparkCompute;
import com.microsoft.azure.hdinsight.sdk.rest.azure.projectarcadia.models.SparkComputeProvisioningState;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageTypeOptionsForCluster;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.util.Optional;

public class ArcadiaSparkCompute extends SparkCluster implements ILogger {
    public static final int ARCADIA_SPARK_SERVICE_PORT = 443;
    @NotNull
    private final ArcadiaWorkSpace workSpace;

    @NotNull
    private final SparkCompute sparkComputeResponse;

    public ArcadiaSparkCompute(@NotNull ArcadiaWorkSpace workSpace, @NotNull SparkCompute sparkComputeResponse) {
        this.workSpace = workSpace;
        this.sparkComputeResponse = sparkComputeResponse;
    }

    public boolean isRunning() {
        if (getProvisioningState() == null) {
            return false;
        }

        return getProvisioningState().equals(SparkComputeProvisioningState.PROVISIONING)
                || getProvisioningState().equals(SparkComputeProvisioningState.SUCCEEDED);
    }

    @Nullable
    public SparkComputeProvisioningState getProvisioningState() {
        return this.sparkComputeResponse.provisioningState();
    }

    @NotNull
    @Override
    public String getState() {
        return Optional.ofNullable(getProvisioningState()).map(state -> state.toString()).orElse("Unknown");
    }

    @NotNull
    @Override
    public String getName() {
        return this.sparkComputeResponse.name();
    }

    @NotNull
    @Override
    public String getClusterIdForConfiguration() {
        return String.format("%s@%s", getName(), workSpace.getName());
    }

    // This title is shown for spark compute list in run configuration dialog
    @NotNull
    @Override
    public String getTitle() {
        if (getState().equalsIgnoreCase(SparkComputeProvisioningState.SUCCEEDED.toString())) {
            return getClusterIdForConfiguration();
        } else {
            return String.format("%s@%s [%s]", getName(), workSpace.getName(), getState());
        }
    }

    // This title is shown for spark compute node in Azure Explorer
    @NotNull
    public String getTitleForNode() {
        if (getState().equalsIgnoreCase(SparkComputeProvisioningState.SUCCEEDED.toString())) {
            return getName();
        } else {
            return String.format("%s [%s]", getName(), getState());
        }
    }

    @Nullable
    @Override
    public String getConnectionUrl() {
        // Comment the following codes since the "spark" field replacing with "dev" field in API
        // which causes connection URL returns null
//        if (this.workSpace.getSparkUrl() == null) {
//            return null;
//        }

        try {
            // TODO: Enable it when the workspace's Spark URL is usable.
            // return new URIBuilder(this.workSpace.getSparkUrl()).setPort(ARCADIA_SPARK_SERVICE_PORT).build().toString();

            return new URIBuilder()
                    .setScheme("https")
                    .setHost("arcadia-spark-service-prod."
                            + this.workSpace.getWorkspaceResponse().location() + ".cloudapp.azure.com")
                    .setPort(ARCADIA_SPARK_SERVICE_PORT)
                    .setPath("/versions/2019-01-01/sparkcomputes/" + getName() + "/")
                    .build().toString();
        } catch (URISyntaxException e) {
            log().warn(String.format("Getting connection URL for spark compute %s failed. %s", getName(), ExceptionUtils.getStackTrace(e)));
            return null;
        }
    }

    @NotNull
    @Override
    public SubscriptionDetail getSubscription() {
        return this.workSpace.getSubscription();
    }

    @Nullable
    @Override
    public String getSparkVersion() {
        return this.sparkComputeResponse.sparkVersion();
    }

    @Override
    public SparkSubmitStorageTypeOptionsForCluster getStorageOptionsType() {
        return SparkSubmitStorageTypeOptionsForCluster.ArcadiaSparkCluster;
    }

    @Override
    public SparkSubmitStorageType getDefaultStorageType() {
        return SparkSubmitStorageType.BLOB;
    }

    @NotNull
    public ArcadiaWorkSpace getWorkSpace() {
        return workSpace;
    }

    @Override
    public int compareTo(@NotNull ComparableCluster other) {
        if (this == other) {
            return 0;
        }

        final ArcadiaSparkCompute another = (ArcadiaSparkCompute) other;

        // Compare the workspace name firstly
        final int workspaceCmpResult = this.workSpace.getName().compareToIgnoreCase(another.workSpace.getName());
        if (workspaceCmpResult != 0) {
            return workspaceCmpResult;
        }

        // Then, the compute name
        return this.getName().compareToIgnoreCase(another.getName());
    }
}
