/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.livy.interactive;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public enum SessionState {
    NOT_STARTED,        // Session has not been started
    STARTING,           // Session is starting
    IDLE,               // Session is waiting for input
    BUSY,               // Session is executing a statement
    SHUTTING_DOWN,      // Session is shutting down
    KILLED,             // Session is killed
    ERROR,              // Session errored out
    DEAD,               // Session has exited
    SUCCESS             // Session is successfully stopped
    ;

    @JsonValue
    public String getKind() {
        return name().toLowerCase();
    }

    /**
     * To convert the string to SessionState type with case insensitive
     *
     * @param state Session state string
     * @return SessionKind parsed
     * @throws IllegalArgumentException for no enum value matched
     */
    @JsonCreator
    static public SessionState parse(@NotNull String state) {
        return SessionState.valueOf(state.trim().toUpperCase());
    }
}
