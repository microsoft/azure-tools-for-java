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

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import java.util.Collection;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.rest.ExpandableStringEnum;

/**
 * Defines values for SparkItemGroupState.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SparkItemGroupState extends ExpandableStringEnum<SparkItemGroupState> {
    /** Static value Waiting for SparkItemGroupState. */
    public static final SparkItemGroupState WAITING = fromString("Waiting");

    /** Static value Launch for SparkItemGroupState. */
    public static final SparkItemGroupState LAUNCH = fromString("Launch");

    /** Static value Release for SparkItemGroupState. */
    public static final SparkItemGroupState RELEASE = fromString("Release");

    /** Static value Stable for SparkItemGroupState. */
    public static final SparkItemGroupState STABLE = fromString("Stable");

    /** Static value Idle for SparkItemGroupState. */
    public static final SparkItemGroupState IDLE = fromString("Idle");

    /** Static value Failed for SparkItemGroupState. */
    public static final SparkItemGroupState FAILED = fromString("Failed");

    /** Static value Shutdown for SparkItemGroupState. */
    public static final SparkItemGroupState SHUTDOWN = fromString("Shutdown");

    /** Static value Completed for SparkItemGroupState. */
    public static final SparkItemGroupState COMPLETED = fromString("Completed");

    /**
     * Creates or finds a SparkItemGroupState from its string representation.
     * @param name a name to look for
     * @return the corresponding SparkItemGroupState
     */
    @JsonCreator
    public static SparkItemGroupState fromString(String name) {
        return fromString(name, SparkItemGroupState.class);
    }

    /**
     * @return known SparkItemGroupState values
     */
    public static Collection<SparkItemGroupState> values() {
        return values(SparkItemGroupState.class);
    }
}
