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

import org.apache.commons.lang3.StringUtils;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageTypeOptionsForCluster;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

public class MfaHdiAdditionalClusterDetail extends HDInsightAdditionalClusterDetail implements MfaEspCluster, ILogger {
    private String tenantId;

    public MfaHdiAdditionalClusterDetail(String clusterName, String userName, String password, HDStorageAccount storageAccount) {
        super(clusterName, userName, password, storageAccount);
    }

    @Nullable
    @Override
    public synchronized String getTenantId(){
        if(StringUtils.isNoneBlank(this.tenantId)){
            return this.tenantId;
        }

        try {
            String location = SparkBatchSubmission.getInstance().probeAuthRedirectUrl(getConnectionUrl());
            if (location != null) {
                // Redirect location pattern should be like this:
                // https://login.windows.net/<tenantId>/oauth2/authorize?client_id=<client_UUID>&response_type=code
                //     &scope=User.Read&redirect_uri=https://<cluster_name>.azurehdinsight.net
                //     &nonce=<nonce_UUID>&resource=https://graph.microsoft.com
                //     &state=<state_seed>&response_mode=form_post
                this.tenantId = Arrays.stream(URI.create(location).getPath().split("/"))
                        .skip(1)    // The URI path is starting by `/`
                        .findFirst()
                        .orElse(null);
            }
        } catch (IOException ex) {
            log().warn(String.format("Encounter exception when negotiating request for linked mfa cluster %s", ex));
        }

        return this.tenantId;
    }

    @Override
    public SparkSubmitStorageTypeOptionsForCluster getStorageOptionsType() {
        return SparkSubmitStorageTypeOptionsForCluster.MfaHdiLinkedCluster;
    }
}
