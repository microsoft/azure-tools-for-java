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
 * Defines values for ActivityResult.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ActivityResult extends ExpandableStringEnum<ActivityResult> {
    /** Static value None for ActivityResult. */
    public static final ActivityResult NONE = fromString("None");

    /** Static value Succeeded for ActivityResult. */
    public static final ActivityResult SUCCEEDED = fromString("Succeeded");

    /** Static value Cancelled for ActivityResult. */
    public static final ActivityResult CANCELLED = fromString("Cancelled");

    /** Static value Failed for ActivityResult. */
    public static final ActivityResult FAILED = fromString("Failed");

    /**
     * Creates or finds a ActivityResult from its string representation.
     * @param name a name to look for
     * @return the corresponding ActivityResult
     */
    @JsonCreator
    public static ActivityResult fromString(String name) {
        return fromString(name, ActivityResult.class);
    }

    /**
     * @return known ActivityResult values
     */
    public static Collection<ActivityResult> values() {
        return values(ActivityResult.class);
    }
}
