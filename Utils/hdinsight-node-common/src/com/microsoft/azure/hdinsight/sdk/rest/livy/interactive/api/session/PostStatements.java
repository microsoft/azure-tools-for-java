/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.api.session;

import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

/**
 * Runs a statement in a session.
 *
 * Based on Apache Livy, v0.4.0-incubating, refer to http://livy.incubator.apache.org./docs/0.4.0-incubating/rest-api.html
 *
 * For the following URI:
 *   http://<livy base>/sessions/<sessionId>/statements
 *
 * HTTP Operations Supported
 *   POST
 *
 * Query Parameters Supported
 *   None
 */

public class PostStatements implements IConvertible {
    private String code;    // The code to execute

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
