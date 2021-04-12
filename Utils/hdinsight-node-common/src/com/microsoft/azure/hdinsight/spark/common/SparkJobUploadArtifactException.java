/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

public class SparkJobUploadArtifactException extends SparkJobException {
    public SparkJobUploadArtifactException(String message, Throwable cause) {
        super(message, cause);
    }
}
