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

