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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

/**
 * StatementState represents the state of an execution statement.
 *
 * Based on Apache Livy, v0.4.0-incubating, refer to http://livy.incubator.apache.org./docs/0.4.0-incubating/rest-api.html
 */

public enum StatementState {
    WAITING,        // Statement is enqueued but execution hasn't started
    RUNNING,        // Statement is currently running
    AVAILABLE,      // Statement has a response ready
    ERROR,          // Statement failed
    CANCELLING,     // Statement is being cancelling
    CANCELLED       // Statement is cancelled
    ;

    @JsonValue
    public String getKind() {
        return name().toLowerCase();
    }

    /**
     * To convert the string to StatementState type with case insensitive
     * @param state Statement state string
     * @return statementState parsed
     * @throws IllegalArgumentException for no enum value matched
     */
    @JsonCreator
    static public StatementState parse(@NotNull String state) {
        return StatementState.valueOf(state.trim().toUpperCase());
    }
}
