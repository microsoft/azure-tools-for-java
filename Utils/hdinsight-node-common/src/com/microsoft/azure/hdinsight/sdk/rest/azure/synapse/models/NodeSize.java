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
 * Defines values for NodeSize.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NodeSize extends ExpandableStringEnum<NodeSize> {
    /** Static value None for NodeSize. */
    public static final NodeSize NONE = fromString("None");

    /** Static value Small for NodeSize. */
    public static final NodeSize SMALL = fromString("Small");

    /** Static value Medium for NodeSize. */
    public static final NodeSize MEDIUM = fromString("Medium");

    /** Static value Large for NodeSize. */
    public static final NodeSize LARGE = fromString("Large");

    /**
     * Creates or finds a NodeSize from its string representation.
     * @param name a name to look for
     * @return the corresponding NodeSize
     */
    @JsonCreator
    public static NodeSize fromString(String name) {
        return fromString(name, NodeSize.class);
    }

    /**
     * @return known NodeSize values
     */
    public static Collection<NodeSize> values() {
        return values(NodeSize.class);
    }
}
