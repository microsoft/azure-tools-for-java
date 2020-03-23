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
