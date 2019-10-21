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

import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MfaHdiAdditionalClusterDetail extends HDInsightAdditionalClusterDetail implements MfaEspCluster{
    private String tenantId;

    public MfaHdiAdditionalClusterDetail(String clusterName, String userName, String password, HDStorageAccount storageAccount) {
        super(clusterName, userName, password, storageAccount);
    }

    @Nullable
    @Override
    public synchronized String getTenantId(){
        if(tenantId != null){
            return this.tenantId;
        }

        try {
            String location = SparkBatchSubmission.getInstance().negotiateAuthMethod(getConnectionUrl());
            if (location != null && location.matches(SparkBatchSubmission.probeLocationPattern)) {
                Matcher m = Pattern.compile(SparkBatchSubmission.probeLocationPattern).matcher(location);
                if (m.find()) {
                    this.tenantId = m.group("tenantId");
                }
            }
        } catch (IOException e) {
        }

        return this.tenantId;
    }
}
