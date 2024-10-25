/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models;

import com.azure.core.management.SubResource;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.rest.serializer.JsonFlatten;

/**
 * Azure Storage account information.
 */
@JsonFlatten
@JsonIgnoreProperties(ignoreUnknown = true)
public class StorageAccountInformation extends SubResource {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Properties {
        /**
         * The optional suffix for the storage account.
         */
        @JsonProperty(value = "suffix", access = JsonProperty.Access.WRITE_ONLY)
        private String suffix;

    }

    /**
     * The optional properties for the storage account.
     */
    @JsonProperty(value = "properties", access = JsonProperty.Access.WRITE_ONLY)
    private Properties properties;

    /**
     * The resource name.
     */
    @JsonProperty(value = "name", access = JsonProperty.Access.WRITE_ONLY)
    private String name;

    /**
     * The resource type.
     */
    @JsonProperty(value = "type", access = JsonProperty.Access.WRITE_ONLY)
    private String type;

    /**
     * Get the suffix value.
     *
     * @return the suffix value
     */
    public String suffix() {
        return properties == null ? null : properties.suffix;
    }

    /**
     * Get the name value.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Get the type value.
     *
     * @return the type value
     */
    public String type() {
        return this.type;
    }

}
