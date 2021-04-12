/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.rest.ExpandableStringEnum;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines values for SchedulerState.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SchedulerState extends ExpandableStringEnum<SchedulerState> implements Comparable<SchedulerState> {
    /** Static value Any for SchedulerState. */
    public static final SchedulerState ANY = fromString("Any");

    /** Static value Submitted for SchedulerState. */
    public static final SchedulerState SUBMITTED = fromString("Submitted");

    /** Static value Preparing for SchedulerState. */
    public static final SchedulerState PREPARING = fromString("Preparing");

    /** Static value Queued for SchedulerState. */
    public static final SchedulerState QUEUED = fromString("Queued");

    /** Static value Scheduled for SchedulerState. */
    public static final SchedulerState SCHEDULED = fromString("Scheduled");

    /** Static value Finalizing for SchedulerState. */
    public static final SchedulerState FINALIZING = fromString("Finalizing");

    /** Static value Ended for SchedulerState. */
    public static final SchedulerState ENDED = fromString("Ended");

    public static final Map<SchedulerState, Integer> TO_PRIORITY = new HashMap<SchedulerState, Integer>() {{
        put(ANY, 1);
        put(SUBMITTED, 2);
        put(PREPARING, 3);
        put(QUEUED, 4);
        put(SCHEDULED, 5);
        put(FINALIZING, 6);
        put(ENDED, 7);
    }};

    /**
     * Creates or finds a SchedulerState from its string representation.
     * @param name a name to look for
     * @return the corresponding SchedulerState
     */
    @JsonCreator
    public static SchedulerState fromString(String name) {
        return fromString(name, SchedulerState.class);
    }

    /**
     * @return known SchedulerState values
     */
    public static Collection<SchedulerState> values() {
        return values(SchedulerState.class);
    }

    @Override
    public int compareTo(final SchedulerState other) {
        if (this == other) {
            return 0;
        }

        return TO_PRIORITY.getOrDefault(this, 0).compareTo(TO_PRIORITY.getOrDefault(other, 0));
    }
}
