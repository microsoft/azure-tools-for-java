/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.ProxyResource;

/**
 * The resource model definition for a Azure Resource Manager resource with an etag.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureEntityResource extends ProxyResource {
    /**
     * Resource Etag.
     */
    @JsonProperty(value = "etag", access = JsonProperty.Access.WRITE_ONLY)
    private String etag;

    /**
     * Get resource Etag.
     *
     * @return the etag value
     */
    public String etag() {
        return this.etag;
    }

}
