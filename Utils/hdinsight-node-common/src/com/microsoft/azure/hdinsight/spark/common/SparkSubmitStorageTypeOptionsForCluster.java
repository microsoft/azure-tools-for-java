/**
 * Copyright (c) Microsoft Corporation
 * <p>
 * <p>
 * All rights reserved.
 * <p>
 * <p>
 * MIT License
 * <p>
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p>
 * <p>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.spark.common;

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
            SparkSubmitStorageType.NOT_SUPPORT_STORAGE_TYPE
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
            SparkSubmitStorageType.ADLS_GEN1
    }),

    // cosmos cluster on adl whose storage type is only default_storaget_account
    AzureSparkCosmosClusterWithDefaultStorage(new SparkSubmitStorageType[]{
            SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT
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

    // for unknown type cluster
    ClusterWithFullType(new SparkSubmitStorageType[]{
            SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT,
            SparkSubmitStorageType.SPARK_INTERACTIVE_SESSION,
            SparkSubmitStorageType.BLOB,
            SparkSubmitStorageType.ADLS_GEN1,
            SparkSubmitStorageType.WEBHDFS
    });

    private SparkSubmitStorageType[] optionTypes;

    SparkSubmitStorageTypeOptionsForCluster(SparkSubmitStorageType[] optionTypes) {
        this.optionTypes = optionTypes;
    }

    public SparkSubmitStorageType[] getOptionTypes() {
        return optionTypes;
    }
}
