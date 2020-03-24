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

package com.microsoft.azure.hdinsight.sdk.cluster;

import com.microsoft.azure.hdinsight.common.AbfsUri;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageTypeOptionsForCluster;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.util.regex.Pattern;

public class MfaClusterDetail extends ClusterDetail implements MfaEspCluster, ILogger {

    public MfaClusterDetail(SubscriptionDetail paramSubscription, ClusterRawInfo paramClusterRawInfo, IClusterOperation clusterOperation) {
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
