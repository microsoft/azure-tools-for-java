/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.projectarcadia.common;

import com.microsoft.azure.hdinsight.common.AbfsUri;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.AzureAdAccountDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.ComparableCluster;
import com.microsoft.azure.hdinsight.sdk.cluster.SparkCluster;
import com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models.BigDataPoolProvisioningState;
import com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models.BigDataPoolResourceInfo;
import com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models.DataLakeStorageAccountDetails;
import com.microsoft.azure.hdinsight.sdk.storage.ADLSGen2StorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageTypeOptionsForCluster;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.net.URI;
import java.util.Optional;

public class ArcadiaSparkCompute extends SparkCluster implements AzureAdAccountDetail, ILogger {
    public static final String DATA_PLANE_API_VERSION = "2019-11-01-preview";

    @NotNull
    private final ArcadiaWorkSpace workSpace;

    @NotNull
    private final BigDataPoolResourceInfo sparkComputeResponse;

    public ArcadiaSparkCompute(@NotNull ArcadiaWorkSpace workSpace, @NotNull BigDataPoolResourceInfo sparkComputeResponse) {
        this.workSpace = workSpace;
        this.sparkComputeResponse = sparkComputeResponse;
    }

    public boolean isRunning() {
        if (getProvisioningState() == null) {
            return false;
        }

        return getProvisioningState().equals(BigDataPoolProvisioningState.PROVISIONING)
                || getProvisioningState().equals(BigDataPoolProvisioningState.SUCCEEDED);
    }

    @Nullable
    public BigDataPoolProvisioningState getProvisioningState() {
        return BigDataPoolProvisioningState.fromString(this.sparkComputeResponse.provisioningState());
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
        return String.format("[%s] %s", workSpace.getName(), getName());
    }

    // This title is shown for spark compute list in run configuration dialog
    @NotNull
    @Override
    public String getTitle() {
        if (getState().equalsIgnoreCase(BigDataPoolProvisioningState.SUCCEEDED.toString())) {
            return getClusterIdForConfiguration();
        } else {
            return String.format("[%s] %s (%s)", workSpace.getName(), getName(), getState());
        }
    }

    // This title is shown for spark compute node in Azure Explorer
    @NotNull
    public String getTitleForNode() {
        if (getState().equalsIgnoreCase(BigDataPoolProvisioningState.SUCCEEDED.toString())) {
            return getName();
        } else {
            return String.format("%s [%s]", getName(), getState());
        }
    }

    @Nullable
    @Override
    public String getConnectionUrl() {
        if (this.workSpace.getSparkUrl() == null) {
            return null;
        }

        return URI.create(workSpace.getSparkUrl())
                .resolve(String.format("/livyApi/versions/%s/sparkPools/%s/", DATA_PLANE_API_VERSION, getName())).toString();
    }

    @NotNull
    @Override
    public Subscription getSubscription() {
        return this.workSpace.getSubscription();
    }

    @Override
    public String getTenantId() {
        return getSubscription().getTenantId();
    }

    @Nullable
    @Override
    public String getSparkVersion() {
        return this.sparkComputeResponse.sparkVersion();
    }

    @Nullable
    @Override
    public IHDIStorageAccount getStorageAccount() {
        DataLakeStorageAccountDetails storageAccountDetails = getWorkSpace().getStorageAccountDetails();
        if (storageAccountDetails == null
                || storageAccountDetails.accountUrl() == null
                || storageAccountDetails.filesystem() == null) {
            log().warn(String.format("Storage account info is invalid for workspace %s. AccountUrl: %s, filesystem: %s.",
                    getWorkSpace().getName(), storageAccountDetails.accountUrl(), storageAccountDetails.filesystem()));
            return null;
        }

        // Sample response:
        // "accountUrl": "https://accountName.dfs.core.windows.net",
        // "filesystem": "fileSystemName"
        URI storageUri = AbfsUri.parse(storageAccountDetails.accountUrl() + "/" + storageAccountDetails.filesystem()).getUri();
        return new ADLSGen2StorageAccount(this, storageUri.getHost(), true, storageAccountDetails.filesystem());
    }

    @Nullable
    @Override
    public String getDefaultStorageRootPath() {
        ADLSGen2StorageAccount storageAccount = (ADLSGen2StorageAccount) getStorageAccount();
        if (storageAccount == null) {
            return null;
        }

        return storageAccount.getStorageRootPath();
    }

    @Override
    public SparkSubmitStorageTypeOptionsForCluster getStorageOptionsType() {
        return SparkSubmitStorageTypeOptionsForCluster.ArcadiaSparkCluster;
    }

    @Override
    public SparkSubmitStorageType getDefaultStorageType() {
        return SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT;
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
