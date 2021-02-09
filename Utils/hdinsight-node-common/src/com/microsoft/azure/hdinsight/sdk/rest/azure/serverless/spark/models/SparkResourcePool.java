/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Full definition of the spark resource pool entity.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SparkResourcePool extends ResourcePool {
    /**
     * The spark resource pool specific properties.
     */
    @JsonProperty(value = "properties")
    private SparkResourcePoolProperties properties;

    /**
     * Get the spark resource pool specific properties.
     *
     * @return the properties value
     */
    public SparkResourcePoolProperties properties() {
        return this.properties;
    }

    /**
     * Set the spark resource pool specific properties.
     *
     * @param properties the properties value to set
     * @return the SparkResourcePool object itself.
     */
    public SparkResourcePool withProperties(SparkResourcePoolProperties properties) {
        this.properties = properties;
        return this;
    }

}
