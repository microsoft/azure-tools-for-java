/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.api.session;

import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;
import com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.Statement;

import java.util.List;

/**
 * Returns all the statements in a session.
 *
 * Based on Apache Livy, v0.4.0-incubating, refer to http://livy.incubator.apache.org./docs/0.4.0-incubating/rest-api.html
 *
 * For the following URI:
 *   http://<livy base>/sessions/<sessionId>/statements
 *
 * HTTP Operations Supported
 *   GET
 *
 * Query Parameters Supported
 *   None
 */

public class GetStatementsResponse implements IConvertible {
    private List<Statement> statements;     // statement list

    public List<Statement> getStatements() {
        return statements;
    }
}
