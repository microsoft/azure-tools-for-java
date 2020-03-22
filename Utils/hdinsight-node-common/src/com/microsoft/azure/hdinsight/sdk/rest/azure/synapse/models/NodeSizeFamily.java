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
