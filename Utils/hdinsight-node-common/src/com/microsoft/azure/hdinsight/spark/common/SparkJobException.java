/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.sdk.common.HDIException;

/**
 * The Base Exception for all Spark Job related exceptions
 */
public class SparkJobException extends HDIException{
    public SparkJobException(String message) {
        super(message);
    }

    public SparkJobException(String message, int errorCode) {
        super(message, errorCode);
    }

    public SparkJobException(String message, String errorLog) {
        super(message, errorLog);
    }

    public SparkJobException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
