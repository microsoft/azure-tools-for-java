/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.rest.serializer.JsonFlatten;

/**
 * The parameters used to add a new Data Lake Store account.
 */
@JsonFlatten
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddDataLakeStoreParameters {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Properties {
        /**
         * The optional suffix for the Data Lake Store account.
         */
        @JsonProperty(value = "suffix")
        private String suffix;
    }

    /**
     * The optional properties for the Data Lake Store account.
     */
    @JsonProperty(value = "properties")
    private Properties properties;

    /**
     * Get the suffix value.
     *
     * @return the suffix value
     */
    public String suffix() {
        return properties == null ? null : properties.suffix;
    }

    /**
     * Set the suffix value.
     *
     * @param suffix the suffix value to set
     * @return the AddDataLakeStoreParameters object itself.
     */
    public AddDataLakeStoreParameters withSuffix(String suffix) {
        if (properties == null) {
            properties = new Properties();
        }

        this.properties.suffix = suffix;
        return this;
    }

}
