/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
