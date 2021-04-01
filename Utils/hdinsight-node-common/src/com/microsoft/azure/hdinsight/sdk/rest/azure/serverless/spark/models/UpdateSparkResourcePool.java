/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

/**
 * The parameters that can be used to update existing Data Lake Analytics spark resource pool. Only update of number of
 * spark workers is allowed.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateSparkResourcePool implements IConvertible {
    /**
     * The spark resource pool specific properties.
     */
    @JsonProperty(value = "properties")
    private UpdateSparkResourcePoolParameters properties;

    /**
     * Get the spark resource pool specific properties.
     *
     * @return the properties value
     */
    public UpdateSparkResourcePoolParameters properties() {
        return this.properties;
    }

    /**
     * Set the spark resource pool specific properties.
     *
     * @param properties the properties value to set
     * @return the UpdateSparkResourcePool object itself.
     */
    public UpdateSparkResourcePool withProperties(UpdateSparkResourcePoolParameters properties) {
        this.properties = properties;
        return this;
    }

}
