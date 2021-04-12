/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
