/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
