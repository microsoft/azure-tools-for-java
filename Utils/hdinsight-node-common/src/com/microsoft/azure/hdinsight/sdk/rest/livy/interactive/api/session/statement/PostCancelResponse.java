/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.api.session.statement;

/**
 * Cancel the specified statement in this session.
 *
 * Based on Apache Livy, v0.4.0-incubating, refer to http://livy.incubator.apache.org./docs/0.4.0-incubating/rest-api.html
 *
 * For the following URI:
 *   http://<livy base>/sessions/<sessionId>/statements/<statementId>/cancel
 *
 * HTTP Operations Supported
 *   POST
 *
 * Query Parameters Supported
 *   None
 */

public class PostCancelResponse {
    private String msg;     // is always "cancelled"

    public String getMsg() {
        return msg;
    }
}
