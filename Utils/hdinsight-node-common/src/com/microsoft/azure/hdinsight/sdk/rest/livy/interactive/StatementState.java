/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
