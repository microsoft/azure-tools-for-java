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
package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.sdk.storage.StorageAccountType;

public enum SparkSubmitStorageTypeOptionsForCluster {
    // cluster detail using blob as default storage type
    ClusterWithBlob(new SparkSubmitStorageType[]{
            SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT,
            SparkSubmitStorageType.BLOB
    }),

    // cluster detail using adls as default storage type
    // or AzureSparkCosmosCluster using adls
    ClusterWithAdls(new SparkSubmitStorageType[]{
            SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT,
            SparkSubmitStorageType.ADLS_GEN1,
            SparkSubmitStorageType.WEBHDFS
    }),

    // cluster detail using adls gen2 as default storage type
    ClusterWithAdlsGen2(new SparkSubmitStorageType[]{
            SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT,
            SparkSubmitStorageType.ADLS_GEN2
    }),

    // cluster detail with unknown storage type
    ClusterWithUnknown(new SparkSubmitStorageType[]{
            SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT,
            SparkSubmitStorageType.SPARK_INTERACTIVE_SESSION
    }),

    // for hdi additional cluster whose storage type can be blob or adls
    HdiAdditionalClusterWithUndetermineStorage(new SparkSubmitStorageType[]{
            SparkSubmitStorageType.SPARK_INTERACTIVE_SESSION,
            SparkSubmitStorageType.BLOB,
            SparkSubmitStorageType.ADLS_GEN1,
            SparkSubmitStorageType.ADLS_GEN2
    }),

    // for hdi additional cluster with reader role and default storage type is blob,adls or adls gen2
    HdiAdditionalClusterForReaderWithBlob(StorageAccountType.BLOB),

    HdiAdditionalClusterForReaderWithADLSGen1(StorageAccountType.ADLS),

    HdiAdditionalClusterForReaderWithADLSGen2(StorageAccountType.ADLSGen2),

    // cosmos cluster on adl whose storage type is only default_storaget_account
    AzureSparkCosmosClusterWithDefaultStorage(new SparkSubmitStorageType[]{
            SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT
    }),

    // New Spark on Cosmos cluster which aligns API with Synapse supports Gen1 and Blob storage for now
    SynapseCosmosSparkCluster(new SparkSubmitStorageType[]{
            SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT,
            SparkSubmitStorageType.BLOB
    }),

    // arcadia cluster now suppports blob
    ArcadiaSparkCluster(new SparkSubmitStorageType[]{
            SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT,
            SparkSubmitStorageType.BLOB
    }),

    // esp cluster supports default storage account
    MfaHdiCluster(new SparkSubmitStorageType[]{
        SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT
    }),

    // linked esp cluster supports default storage account
    MfaHdiLinkedCluster(new SparkSubmitStorageType[]{
            SparkSubmitStorageType.ADLS_GEN2_FOR_OAUTH
    }),

    // sql big data cluster
    BigDataClusterWithWebHdfs(new SparkSubmitStorageType[]{
            SparkSubmitStorageType.SPARK_INTERACTIVE_SESSION,
            SparkSubmitStorageType.WEBHDFS
    }),

    // Cosmos Serverless Spark cluster
    ServerlessClusterWithAccountDefault(new SparkSubmitStorageType[]{
            SparkSubmitStorageType.ADLA_ACCOUNT_DEFAULT_STORAGE
    }),

    // for HDInsight Reader cluster
    HDInsightReaderStorageTypeOptions(new SparkSubmitStorageType[]{
            SparkSubmitStorageType.SPARK_INTERACTIVE_SESSION,
            SparkSubmitStorageType.BLOB,
            SparkSubmitStorageType.ADLS_GEN1,
            SparkSubmitStorageType.ADLS_GEN2,
            SparkSubmitStorageType.WEBHDFS
    }),

    // for unknown type cluster
    ClusterWithFullType(new SparkSubmitStorageType[]{
            SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT,
            SparkSubmitStorageType.SPARK_INTERACTIVE_SESSION,
            SparkSubmitStorageType.BLOB,
            SparkSubmitStorageType.ADLS_GEN1,
            SparkSubmitStorageType.WEBHDFS,
            SparkSubmitStorageType.ADLS_GEN2
    });

    private SparkSubmitStorageType[] optionTypes;

    SparkSubmitStorageTypeOptionsForCluster(SparkSubmitStorageType[] optionTypes) {
        this.optionTypes = optionTypes;
    }

    SparkSubmitStorageTypeOptionsForCluster(StorageAccountType type) {
        switch (type) {
            case BLOB:
                this.optionTypes = new SparkSubmitStorageType[]{
                        SparkSubmitStorageType.BLOB,
                        SparkSubmitStorageType.SPARK_INTERACTIVE_SESSION
                };
                break;
            case ADLS:
                this.optionTypes = new SparkSubmitStorageType[]{
                        SparkSubmitStorageType.ADLS_GEN1,
                        SparkSubmitStorageType.SPARK_INTERACTIVE_SESSION
                };
                break;
            case ADLSGen2:
                this.optionTypes = new SparkSubmitStorageType[]{
                        SparkSubmitStorageType.ADLS_GEN2,
                        SparkSubmitStorageType.SPARK_INTERACTIVE_SESSION
                };
                break;
        }
    }

    public SparkSubmitStorageType[] getOptionTypes() {
        return optionTypes;
    }
}
