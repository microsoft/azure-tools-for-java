/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public enum SparkSubmitStorageType {
    BLOB("Use Azure Blob to upload"),
    DEFAULT_STORAGE_ACCOUNT("Use cluster default storage account to upload"),
    SPARK_INTERACTIVE_SESSION("Use Spark interactive session to upload"),
    ADLS_GEN1("Use ADLS Gen 1 to upload"),
    ADLS_GEN2("Use ADLS Gen 2 to upload"),
    ADLS_GEN2_FOR_OAUTH("Use ADLS Gen2 with azure account to upload"),
    WEBHDFS("Use WebHDFS to upload"),
    ADLA_ACCOUNT_DEFAULT_STORAGE("Use Cosmos account default storage to upload"),
    NOT_SUPPORT_STORAGE_TYPE("Unknown storage type");

    private String description;
    SparkSubmitStorageType(@NotNull String title) {
        description = title;
    }

    @NotNull
    public String getDescription() {
        return description;
    }
}

