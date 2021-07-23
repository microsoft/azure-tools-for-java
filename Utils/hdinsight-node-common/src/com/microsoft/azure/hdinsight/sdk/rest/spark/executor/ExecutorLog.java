/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.spark.executor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

/**
 * executor Log
 * Based on Spark 2.1.0, refer to http://spark.apache.org/docs/latest/monitoring.html
 *
 *   http://[spark http address:port]/applications/[app-id]/executors
 *
 * HTTP Operations Supported
 *   GET
 *
 * Query Parameters Supported
 *   None
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutorLog implements IConvertible {
    private String stdout;
    private String stderr;

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }
}
