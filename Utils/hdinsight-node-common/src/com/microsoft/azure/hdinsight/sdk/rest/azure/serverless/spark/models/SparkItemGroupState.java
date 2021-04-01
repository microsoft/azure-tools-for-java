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
