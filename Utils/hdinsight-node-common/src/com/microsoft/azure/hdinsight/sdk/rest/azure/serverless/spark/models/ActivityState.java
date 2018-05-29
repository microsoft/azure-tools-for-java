/**
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

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Defines values for ActivityState.
 */
public enum ActivityState {
    /** Enum value Accepted. */
    ACCEPTED("Accepted"),

    /** Enum value Compiling. */
    COMPILING("Compiling"),

    /** Enum value Ended. */
    ENDED("Ended"),

    /** Enum value New. */
    NEW("New"),

    /** Enum value Queued. */
    QUEUED("Queued"),

    /** Enum value Running. */
    RUNNING("Running"),

    /** Enum value Scheduling. */
    SCHEDULING("Scheduling"),

    /** Enum value Scheduled. */
    SCHEDULED("Scheduled"),

    /** Enum value Starting. */
    STARTING("Starting"),

    /** Enum value Paused. */
    PAUSED("Paused"),

    /** Enum value WaitingForCapacity. */
    WAITING_FOR_CAPACITY("WaitingForCapacity");

    /** The actual serialized value for a ActivityState instance. */
    private String value;

    ActivityState(String value) {
        this.value = value;
    }

    /**
     * Parses a serialized value to a ActivityState instance.
     *
     * @param value the serialized value to parse.
     * @return the parsed ActivityState object, or null if unable to parse.
     */
    @JsonCreator
    public static ActivityState fromString(String value) {
        ActivityState[] items = ActivityState.values();
        for (ActivityState item : items) {
            if (item.toString().equalsIgnoreCase(value)) {
                return item;
            }
        }
        return null;
    }

    @JsonValue
    @Override
    public String toString() {
        return this.value;
    }
}
