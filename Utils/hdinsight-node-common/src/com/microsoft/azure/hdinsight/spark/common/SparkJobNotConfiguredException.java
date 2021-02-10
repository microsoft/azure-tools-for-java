/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import java.net.UnknownServiceException;

public class SparkJobNotConfiguredException extends UnknownServiceException {
    public SparkJobNotConfiguredException(String message) {
        super(message);
    }
}
