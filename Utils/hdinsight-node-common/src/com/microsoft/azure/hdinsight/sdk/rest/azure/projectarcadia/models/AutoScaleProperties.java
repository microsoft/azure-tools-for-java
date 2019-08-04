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

package com.microsoft.azure.hdinsight.sdk.rest.azure.projectarcadia.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The auto scale properties for the spark compute.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoScaleProperties {
    /**
     * Depicts whether autoscale is enabled for the spark compute.
     */
    @JsonProperty(value = "enabled")
    private Boolean enabled;

    /**
     * Minimum number of nodes for spark compute.
     */
    @JsonProperty(value = "minNodeCount")
    private Integer minNodeCount;

    /**
     * Maximum number of nodes for spark compute.
     */
    @JsonProperty(value = "maxNodeCount")
    private Integer maxNodeCount;

    /**
     * Get depicts whether autoscale is enabled for the spark compute.
     *
     * @return the enabled value
     */
    public Boolean enabled() {
        return this.enabled;
    }

    /**
     * Set depicts whether autoscale is enabled for the spark compute.
     *
     * @param enabled the enabled value to set
     * @return the AutoScaleProperties object itself.
     */
    public AutoScaleProperties withEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Get minimum number of nodes for spark compute.
     *
     * @return the minNodeCount value
     */
    public Integer minNodeCount() {
        return this.minNodeCount;
    }

    /**
     * Set minimum number of nodes for spark compute.
     *
     * @param minNodeCount the minNodeCount value to set
     * @return the AutoScaleProperties object itself.
     */
    public AutoScaleProperties withMinNodeCount(Integer minNodeCount) {
        this.minNodeCount = minNodeCount;
        return this;
    }

    /**
     * Get maximum number of nodes for spark compute.
     *
     * @return the maxNodeCount value
     */
    public Integer maxNodeCount() {
        return this.maxNodeCount;
    }

    /**
     * Set maximum number of nodes for spark compute.
     *
     * @param maxNodeCount the maxNodeCount value to set
     * @return the AutoScaleProperties object itself.
     */
    public AutoScaleProperties withMaxNodeCount(Integer maxNodeCount) {
        this.maxNodeCount = maxNodeCount;
        return this;
    }

}
