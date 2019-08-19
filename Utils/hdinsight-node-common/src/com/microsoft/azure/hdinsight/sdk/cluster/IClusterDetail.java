/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.sdk.cluster;


import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageTypeOptionsForCluster;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.io.IOException;
import java.util.List;

public interface IClusterDetail {

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

    SubscriptionDetail getSubscription();

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
    default IHDIStorageAccount getStorageAccount() {
        return null;
    }

    default List<HDStorageAccount> getAdditionalStorageAccounts() {
        return null;
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