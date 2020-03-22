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
package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Spark specific resource pool information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SparkResourcePoolProperties {
    /**
     * The sparkResourceCollection property.
     */
    @JsonProperty(value = "sparkResourceCollection", access = JsonProperty.Access.WRITE_ONLY)
    private List<SparkResourcePoolItemProperties> sparkResourceCollection;

    /**
     * State of the Activity. Possible values include: 'New', 'Queued', 'Scheduling', 'Starting', 'Launching',
     * 'Running', 'Rediscovering', 'Ending', 'Ended'.
     */
    @JsonProperty(value = "state")
    private SparkResourcePoolState state;

    /**
     * Definition of Spark Uri Collection.
     */
    @JsonProperty(value = "sparkUriCollection", access = JsonProperty.Access.WRITE_ONLY)
    private SparkResourcePoolUriItemProperties sparkUriCollection;

    /**
     * Get the sparkResourceCollection value.
     *
     * @return the sparkResourceCollection value
     */
    public List<SparkResourcePoolItemProperties> sparkResourceCollection() {
        return this.sparkResourceCollection;
    }

    /**
     * Get state of the Activity. Possible values include: 'New', 'Queued', 'Scheduling', 'Starting', 'Launching', 'Running', 'Rediscovering', 'Ending', 'Ended'.
     *
     * @return the state value
     */
    public SparkResourcePoolState state() {
        return this.state;
    }

    /**
     * Set state of the Activity. Possible values include: 'New', 'Queued', 'Scheduling', 'Starting', 'Launching', 'Running', 'Rediscovering', 'Ending', 'Ended'.
     *
     * @param state the state value to set
     * @return the SparkResourcePoolProperties object itself.
     */
    public SparkResourcePoolProperties withState(SparkResourcePoolState state) {
        this.state = state;
        return this;
    }

    /**
     * Get definition of Spark Uri Collection.
     *
     * @return the sparkUriCollection value
     */
    public SparkResourcePoolUriItemProperties sparkUriCollection() {
        return this.sparkUriCollection;
    }

}
