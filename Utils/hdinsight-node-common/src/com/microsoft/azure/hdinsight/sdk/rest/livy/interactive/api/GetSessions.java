/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.api;

import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

/**
 * The request body to get all Livy active interactive sessions
 *
 * Based on Apache Livy, v0.4.0-incubating, refer to http://livy.incubator.apache.org./docs/0.4.0-incubating/rest-api.html
 *
 * Use the following URI:
 *   http://<livy base>/sessions
 *
 * HTTP Operations Supported
 *   GET
 *
 * Query Parameters Supported
 *   None
 *
 * Response Type
 *   @see GetSessionsResponse
 */
public class GetSessions implements IConvertible {
    private int from;       // The start index to fetch sessions
    private int size;       // Number of sessions to fetch

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
