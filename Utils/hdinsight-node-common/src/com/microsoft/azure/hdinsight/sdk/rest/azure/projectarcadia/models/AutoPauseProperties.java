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
 * The auto pause properties for the spark compute.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoPauseProperties {
    /**
     * Depicts whether autopause is enabled for the spark compute.
     */
    @JsonProperty(value = "enabled")
    private Boolean enabled;

    /**
     * Idle time in minutes after which spark compute is paused.
     */
    @JsonProperty(value = "delayInMinutes")
    private Integer delayInMinutes;

    /**
     * Get depicts whether autopause is enabled for the spark compute.
     *
     * @return the enabled value
     */
    public Boolean enabled() {
        return this.enabled;
    }

    /**
     * Set depicts whether autopause is enabled for the spark compute.
     *
     * @param enabled the enabled value to set
     * @return the AutoPauseProperties object itself.
     */
    public AutoPauseProperties withEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Get idle time in minutes after which spark compute is paused.
     *
     * @return the delayInMinutes value
     */
    public Integer delayInMinutes() {
        return this.delayInMinutes;
    }

    /**
     * Set idle time in minutes after which spark compute is paused.
     *
     * @param delayInMinutes the delayInMinutes value to set
     * @return the AutoPauseProperties object itself.
     */
    public AutoPauseProperties withDelayInMinutes(Integer delayInMinutes) {
        this.delayInMinutes = delayInMinutes;
        return this;
    }

}
