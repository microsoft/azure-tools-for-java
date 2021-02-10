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
 * Defines values for EntityType.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class EntityType extends ExpandableStringEnum<EntityType> {
    /** Static value ResourcePools for EntityType. */
    public static final EntityType RESOURCE_POOLS = fromString("ResourcePools");

    /** Static value BatchJobs for EntityType. */
    public static final EntityType BATCH_JOBS = fromString("BatchJobs");

    /** Static value StreamingJobs for EntityType. */
    public static final EntityType STREAMING_JOBS = fromString("StreamingJobs");

    /**
     * Creates or finds a EntityType from its string representation.
     * @param name a name to look for
     * @return the corresponding EntityType
     */
    @JsonCreator
    public static EntityType fromString(String name) {
        return fromString(name, EntityType.class);
    }

    /**
     * @return known EntityType values
     */
    public static Collection<EntityType> values() {
        return values(EntityType.class);
    }
}
