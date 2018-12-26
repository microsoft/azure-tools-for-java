/**
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
 *
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import java.util.Collection;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.rest.ExpandableStringEnum;

/**
 * Defines values for SchedulerState.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SchedulerState extends ExpandableStringEnum<SchedulerState> {
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
}
