/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;

import java.io.IOException;
import java.util.List;

public interface IClusterOperation {

    /**
     * list hdinsight cluster
     * @param subscription
     * @return cluster raw data info
     * @throws IOException
     */
    List<ClusterRawInfo> listCluster(SubscriptionDetail subscription) throws IOException, HDIException, AzureCmdException;

    /**
     * get cluster configuration including http username, password, storage and additional storage account
     * @param subscription
     * @param clusterId
     * @return cluster configuration info
     * @throws IOException
     */
    ClusterConfiguration getClusterConfiguration(SubscriptionDetail subscription, String clusterId) throws IOException, HDIException, AzureCmdException;
}
