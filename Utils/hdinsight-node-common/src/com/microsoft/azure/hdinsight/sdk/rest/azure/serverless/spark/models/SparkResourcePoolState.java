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
 * Defines values for SparkResourcePoolState.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SparkResourcePoolState extends ExpandableStringEnum<SparkResourcePoolState> {
    /** Static value New for SparkResourcePoolState. */
    public static final SparkResourcePoolState NEW = fromString("New");

    /** Static value Queued for SparkResourcePoolState. */
    public static final SparkResourcePoolState QUEUED = fromString("Queued");

    /** Static value Scheduling for SparkResourcePoolState. */
    public static final SparkResourcePoolState SCHEDULING = fromString("Scheduling");

    /** Static value Starting for SparkResourcePoolState. */
    public static final SparkResourcePoolState STARTING = fromString("Starting");

    /** Static value Launching for SparkResourcePoolState. */
    public static final SparkResourcePoolState LAUNCHING = fromString("Launching");

    /** Static value Running for SparkResourcePoolState. */
    public static final SparkResourcePoolState RUNNING = fromString("Running");

    /** Static value Rediscovering for SparkResourcePoolState. */
    public static final SparkResourcePoolState REDISCOVERING = fromString("Rediscovering");

    /** Static value Ending for SparkResourcePoolState. */
    public static final SparkResourcePoolState ENDING = fromString("Ending");

    /** Static value Ended for SparkResourcePoolState. */
    public static final SparkResourcePoolState ENDED = fromString("Ended");

    /**
     * Creates or finds a SparkResourcePoolState from its string representation.
     * @param name a name to look for
     * @return the corresponding SparkResourcePoolState
     */
    @JsonCreator
    public static SparkResourcePoolState fromString(String name) {
        return fromString(name, SparkResourcePoolState.class);
    }

    /**
     * @return known SparkResourcePoolState values
     */
    public static Collection<SparkResourcePoolState> values() {
        return values(SparkResourcePoolState.class);
    }
}
