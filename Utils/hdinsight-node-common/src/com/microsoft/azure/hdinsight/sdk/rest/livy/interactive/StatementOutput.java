/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.livy.interactive;

import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

import java.util.List;
import java.util.Map;

/**
 * A sratementOutput represents the output of an execution statement.
 *
 * Based on Apache Livy, v0.4.0-incubating, refer to http://livy.incubator.apache.org./docs/0.4.0-incubating/rest-api.html
 */

public class StatementOutput implements IConvertible {
    private String              status;             // Execution status
    private int                 execution_count;    // A monotonically increasing number
    private String              ename;              // Error name, only for "error" status
    private String              evalue;             // Error value, only for "error" status
    private List<String>        traceback;          // Error traceback lines, only for "error" status
    private Map<String, String> data;               // Statement output. An object mapping a mime type to the result.
                                                    // If the mime type is ``application/json``, the value is a
                                                    // JSON value

    public String getStatus() {
        return status;
    }

    public int getExecution_count() {
        return execution_count;
    }

    public String getEname() {
        return ename;
    }

    public String getEvalue() {
        return evalue;
    }

    public List<String> getTraceback() {
        return traceback;
    }

    public Map<String, String> getData() {
        return data;
    }
}
