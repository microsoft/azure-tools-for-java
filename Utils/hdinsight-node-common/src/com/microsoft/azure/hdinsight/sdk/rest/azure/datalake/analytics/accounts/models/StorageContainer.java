/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.rest.serializer.JsonFlatten;
import com.microsoft.azure.SubResource;

/**
 * Azure Storage blob container information.
 */
@JsonFlatten
@JsonIgnoreProperties(ignoreUnknown = true)
public class StorageContainer extends SubResource {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Properties {
        /**
         * The last modified time of the blob container.
         */
        @JsonProperty(value = "lastModifiedTime", access = JsonProperty.Access.WRITE_ONLY)
        private String lastModifiedTime;
    }

    /**
     * The properties
     */
    @JsonProperty(value = "properties")
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
     * Get the lastModifiedTime value.
     *
     * @return the lastModifiedTime value
     */
    public String lastModifiedTime() {
        return this.properties == null ? null : properties.lastModifiedTime;
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
