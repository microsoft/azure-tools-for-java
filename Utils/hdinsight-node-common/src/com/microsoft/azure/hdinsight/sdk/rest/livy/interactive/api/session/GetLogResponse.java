/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.api.session;

import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

import java.util.List;

/**
 * The response body after getting the log lines from this session.
 *
 * Based on Apache Livy, v0.4.0-incubating, refer to http://livy.incubator.apache.org./docs/0.4.0-incubating/rest-api.html
 *
 * For the following URI:
 *   http://<livy base>/sessions/<sessionId>/log
 *
 * HTTP Operations Supported
 *   GET
 *
 * Query Parameters Supported
 *   None
 */

public class GetLogResponse implements IConvertible {
    private int             id;     // The session id
    private int             from;   // Offset from start of log
    private int             size;   // Max number of log lines
    private List<String>    log;    // The log lines

    public int getId() {
        return id;
    }

    public int getFrom() {
        return from;
    }

    public int getSize() {
        return size;
    }

    public List<String> getLog() {
        return log;
    }
}
