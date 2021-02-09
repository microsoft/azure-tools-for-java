/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.jobs;

import com.microsoft.azure.hdinsight.sdk.common.HDIException;


public class HDINetException extends HDIException {

    public HDINetException(int code, String message, Throwable throwable ) {
        super(message, throwable);
    }
}
