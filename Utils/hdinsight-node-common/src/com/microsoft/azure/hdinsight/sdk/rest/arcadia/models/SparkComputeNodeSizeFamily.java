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

package com.microsoft.azure.hdinsight.sdk.rest.arcadia.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Defines values for SparkComputeNodeSizeFamily.
 */
public enum SparkComputeNodeSizeFamily {
    /** Enum value Small. */
    SMALL("Small"),

    /** Enum value Medium. */
    MEDIUM("Medium"),

    /** Enum value Large. */
    LARGE("Large");

    /** The actual serialized value for a SparkComputeNodeSizeFamily instance. */
    private String value;

    SparkComputeNodeSizeFamily(String value) {
        this.value = value;
    }

    /**
     * Parses a serialized value to a SparkComputeNodeSizeFamily instance.
     *
     * @param value the serialized value to parse.
     * @return the parsed SparkComputeNodeSizeFamily object, or null if unable to parse.
     */
    @JsonCreator
    public static SparkComputeNodeSizeFamily fromString(String value) {
        SparkComputeNodeSizeFamily[] items = SparkComputeNodeSizeFamily.values();
        for (SparkComputeNodeSizeFamily item : items) {
            if (item.toString().equalsIgnoreCase(value)) {
                return item;
            }
        }
        return null;
    }

    @JsonValue
    @Override
    public String toString() {
        return this.value;
    }
}
