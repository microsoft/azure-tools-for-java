/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;


import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageTypeOptionsForCluster;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public interface IClusterDetail extends ComparableCluster {

    default boolean isEmulator() {
        return false;
    }
    default boolean isConfigInfoAvailable() {
        return false;
    }

    String getName();

    String getTitle();

    default String getClusterIdForConfiguration() {
        return getName();
    }

    default String getState() {
        return null;
    }

    default String getLocation() {
        return null;
    }

    String getConnectionUrl();

    default String getCreateDate() {
        return null;
    }

    default ClusterType getType() {
        return null;
    }

    default String getVersion() {
        return null;
    }

    Subscription getSubscription();

    default int getDataNodes() {
        return 0;
    }

    default String getHttpUserName() throws HDIException {
        return null;
    }

    default String getHttpPassword() throws HDIException {
        return null;
    }

    default String getOSType() {
        return null;
    }

    default String getResourceGroup() {
        return null;
    }

    @Nullable
    default String getDefaultStorageRootPath() {
        return null;
    }

    @Nullable
    default IHDIStorageAccount getStorageAccount() {
        return null;
    }

    default List<HDStorageAccount> getAdditionalStorageAccounts() {
        return Collections.emptyList();
    }

    default void getConfigurationInfo() throws IOException, HDIException, AzureCmdException {
    }

    default String getSparkVersion() {
        return null;
    }

    default SparkSubmitStorageType getDefaultStorageType(){
        return SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT;
    }

    default SparkSubmitStorageTypeOptionsForCluster getStorageOptionsType(){
        return SparkSubmitStorageTypeOptionsForCluster.ClusterWithFullType;
    }
}
