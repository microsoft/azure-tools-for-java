/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.rest.serializer.JsonFlatten;

/**
 * The parameters used to add a new Azure Storage account.
 */
@JsonFlatten
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddStorageAccountParameters {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Properties {
        /**
         * The access key associated with this Azure Storage account that will be
         * used to connect to it.
         */
        @JsonProperty(value = "accessKey", required = true)
        private String accessKey;

        /**
         * The optional suffix for the storage account.
         */
        @JsonProperty(value = "suffix")
        private String suffix;

    }

    /**
     * The properties for the storage account.
     */
    @JsonProperty(value = "properties")
    private Properties properties;

    /**
     * Get the accessKey value.
     *
     * @return the accessKey value
     */
    public String accessKey() {
        return properties == null ? null : properties.accessKey;
    }

    /**
     * Set the accessKey value.
     *
     * @param accessKey the accessKey value to set
     * @return the AddStorageAccountParameters object itself.
     */
    public AddStorageAccountParameters withAccessKey(String accessKey) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.accessKey = accessKey;
        return this;
    }

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
     * @return the AddStorageAccountParameters object itself.
     */
    public AddStorageAccountParameters withSuffix(String suffix) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.suffix = suffix;
        return this;
    }

}
