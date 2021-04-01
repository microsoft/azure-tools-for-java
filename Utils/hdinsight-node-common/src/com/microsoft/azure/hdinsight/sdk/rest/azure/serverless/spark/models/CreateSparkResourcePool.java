/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

/**
 * Parameters used to submit a new Data Lake Analytics resource pool creation request.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateSparkResourcePool implements IConvertible {
    /**
     * Friendly name of the resource pool to submit.
     */
    @JsonProperty(value = "name", required = true)
    private String name;

    /**
     * The spark resource pool specific properties.
     */
    @JsonProperty(value = "properties", required = true)
    private CreateSparkResourcePoolParameters properties;

    /**
     * Get friendly name of the resource pool to submit.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Set friendly name of the resource pool to submit.
     *
     * @param name the name value to set
     * @return the CreateSparkResourcePool object itself.
     */
    public CreateSparkResourcePool withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the spark resource pool specific properties.
     *
     * @return the properties value
     */
    public CreateSparkResourcePoolParameters properties() {
        return this.properties;
    }

    /**
     * Set the spark resource pool specific properties.
     *
     * @param properties the properties value to set
     * @return the CreateSparkResourcePool object itself.
     */
    public CreateSparkResourcePool withProperties(CreateSparkResourcePoolParameters properties) {
        this.properties = properties;
        return this;
    }

}
