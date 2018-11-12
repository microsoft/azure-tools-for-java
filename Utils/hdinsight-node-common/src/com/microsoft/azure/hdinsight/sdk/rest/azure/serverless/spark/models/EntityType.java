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
