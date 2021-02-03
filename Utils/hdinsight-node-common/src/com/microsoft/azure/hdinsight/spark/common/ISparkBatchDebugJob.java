/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import rx.Observable;

import java.io.IOException;

public interface ISparkBatchDebugJob extends ISparkBatchJob {
    /**
     * Get Spark Batch job driver debugging port number
     *
     * @return Spark driver node debugging port
     * @throws IOException exceptions for the driver debugging port not found
     */
    Observable<Integer> getSparkDriverDebuggingPort() throws IOException;
}
