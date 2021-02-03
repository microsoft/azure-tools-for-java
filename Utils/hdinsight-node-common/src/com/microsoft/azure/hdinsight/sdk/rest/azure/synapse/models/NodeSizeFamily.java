/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models;

import java.util.Collection;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.rest.ExpandableStringEnum;

/**
 * Defines values for NodeSizeFamily.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NodeSizeFamily extends ExpandableStringEnum<NodeSizeFamily> {
    /** Static value None for NodeSizeFamily. */
    public static final NodeSizeFamily NONE = fromString("None");

    /** Static value MemoryOptimized for NodeSizeFamily. */
    public static final NodeSizeFamily MEMORY_OPTIMIZED = fromString("MemoryOptimized");

    /**
     * Creates or finds a NodeSizeFamily from its string representation.
     * @param name a name to look for
     * @return the corresponding NodeSizeFamily
     */
    @JsonCreator
    public static NodeSizeFamily fromString(String name) {
        return fromString(name, NodeSizeFamily.class);
    }

    /**
     * @return known NodeSizeFamily values
     */
    public static Collection<NodeSizeFamily> values() {
        return values(NodeSizeFamily.class);
    }
}
