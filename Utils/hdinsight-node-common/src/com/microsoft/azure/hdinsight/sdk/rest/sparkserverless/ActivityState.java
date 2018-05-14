/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.sdk.rest.sparkserverless;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public enum ActivityState {
    ACCEPTED,               // Static value Accepted for ActivityState
    COMPILING,              // Static value Compiling for ActivityState
    ENDED,                  // Static value Ended for ActivityState
    NEW,                    // Static value New for ActivityState
    QUEUED,                 // Static value Queued for ActivityState
    RUNNING,                // Static value Running for ActivityState
    SCHEDULING,             // Static value Scheduling for ActivityState
    STARTING,               // Static value Starting for ActivityState
    PAUSED,                 // Static value Paused for ActivityState
    WAITING_FOR_CAPACITY;    // Static value WaitingForCapacity for ActivityState

    @NotNull
    @JsonValue
    public String getKind() {
        return name().toLowerCase();
    }

    /**
     * To convert the string to ActivityState type with case insensitive
     *
     * @param state activity state string
     * @return ActivityState parsed
     * @throws IllegalArgumentException for no enum value matched
     */
    @NotNull
    @JsonCreator
    static public ActivityState parse(@NotNull String state) throws IllegalArgumentException {
        return ActivityState.valueOf(state.trim().toUpperCase());
    }
}
