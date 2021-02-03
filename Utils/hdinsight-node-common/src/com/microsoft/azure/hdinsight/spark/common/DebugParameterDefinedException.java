/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

/**
 * The Exception for debugging parameter already defined in configuration
 */
public class DebugParameterDefinedException extends SparkJobException {
    public DebugParameterDefinedException(String message) {
        super(message);
    }
}
