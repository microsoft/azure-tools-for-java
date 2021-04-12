/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common.logger;


import org.slf4j.LoggerFactory;

/**
 * Base logger class
 */
public interface ILogger {
    default org.slf4j.Logger log() {
        return LoggerFactory.getLogger(getClass());
    }
}
