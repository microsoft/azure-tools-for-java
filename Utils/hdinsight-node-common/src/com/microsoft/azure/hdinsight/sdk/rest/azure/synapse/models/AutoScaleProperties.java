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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Spark pool auto-scaling properties.
 * Auto-scaling properties of a Big Data pool powered by Apache Spark.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoScaleProperties {
    /**
     * The minimum number of nodes the Big Data pool can support.
     */
    @JsonProperty(value = "minNodeCount")
    private Integer minNodeCount;

    /**
     * Whether automatic scaling is enabled for the Big Data pool.
     */
    @JsonProperty(value = "enabled")
    private Boolean enabled;

    /**
     * The maximum number of nodes the Big Data pool can support.
     */
    @JsonProperty(value = "maxNodeCount")
    private Integer maxNodeCount;

    /**
     * Get the minimum number of nodes the Big Data pool can support.
     *
     * @return the minNodeCount value
     */
    public Integer minNodeCount() {
        return this.minNodeCount;
    }

    /**
     * Set the minimum number of nodes the Big Data pool can support.
     *
     * @param minNodeCount the minNodeCount value to set
     * @return the AutoScaleProperties object itself.
     */
    public AutoScaleProperties withMinNodeCount(Integer minNodeCount) {
        this.minNodeCount = minNodeCount;
        return this;
    }

    /**
     * Get whether automatic scaling is enabled for the Big Data pool.
     *
     * @return the enabled value
     */
    public Boolean enabled() {
        return this.enabled;
    }

    /**
     * Set whether automatic scaling is enabled for the Big Data pool.
     *
     * @param enabled the enabled value to set
     * @return the AutoScaleProperties object itself.
     */
    public AutoScaleProperties withEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Get the maximum number of nodes the Big Data pool can support.
     *
     * @return the maxNodeCount value
     */
    public Integer maxNodeCount() {
        return this.maxNodeCount;
    }

    /**
     * Set the maximum number of nodes the Big Data pool can support.
     *
     * @param maxNodeCount the maxNodeCount value to set
     * @return the AutoScaleProperties object itself.
     */
    public AutoScaleProperties withMaxNodeCount(Integer maxNodeCount) {
        this.maxNodeCount = maxNodeCount;
        return this;
    }

}
