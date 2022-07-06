/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import com.microsoft.azure.hdinsight.common.AbfsUri;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageTypeOptionsForCluster;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.util.regex.Pattern;

public class MfaClusterDetail extends ClusterDetail implements MfaEspCluster, ILogger {

    public MfaClusterDetail(Subscription paramSubscription, ClusterRawInfo paramClusterRawInfo, IClusterOperation clusterOperation) {
        super(paramSubscription, paramClusterRawInfo, clusterOperation);
    }

    @Nullable
    @Override
    public String getDefaultStorageRootPath() {
        String storageRootPath = super.getDefaultStorageRootPath();

        if (storageRootPath == null) {
            return null;
        }

        // check login status and get the login user name
        try {
            if (Pattern.compile(AbfsUri.AdlsGen2PathPattern).matcher(storageRootPath).matches()) {
                storageRootPath = String.format("%s/%s", storageRootPath, getUserPath());
            }

            return storageRootPath;
        } catch (Exception ex) {
            log().warn(String.format("Get default storage root path for mfa cluster encounter %s", ex));
        }

        return null;
    }

    @Override
    public SparkSubmitStorageTypeOptionsForCluster getStorageOptionsType() {
        return SparkSubmitStorageTypeOptionsForCluster.MfaHdiCluster;
    }

    @Nullable
    @Override
    public String getTenantId() {
        return getSubscription().getTenantId();
    }
}
