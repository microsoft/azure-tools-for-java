/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.ProxyResource;

/**
 * The resource model definition for a ARM tracked top level resource.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackedResource extends ProxyResource {
    /**
     * Resource tags.
     */
    @JsonProperty(value = "tags")
    private Map<String, String> tags;

    /**
     * The geo-location where the resource lives.
     */
    @JsonProperty(value = "location", required = true)
    private String location;

    /**
     * Get resource tags.
     *
     * @return the tags value
     */
    public Map<String, String> tags() {
        return this.tags;
    }

    /**
     * Set resource tags.
     *
     * @param tags the tags value to set
     * @return the TrackedResource object itself.
     */
    public TrackedResource withTags(Map<String, String> tags) {
        this.tags = tags;
        return this;
    }

    /**
     * Get the geo-location where the resource lives.
     *
     * @return the location value
     */
    public String location() {
        return this.location;
    }

    /**
     * Set the geo-location where the resource lives.
     *
     * @param location the location value to set
     * @return the TrackedResource object itself.
     */
    public TrackedResource withLocation(String location) {
        this.location = location;
        return this;
    }

}
