/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The display information for a particular operation.
 */
public class OperationDisplay {
    /**
     * The resource provider of the operation.
     */
    @JsonProperty(value = "provider", access = JsonProperty.Access.WRITE_ONLY)
    private String provider;

    /**
     * The resource type of the operation.
     */
    @JsonProperty(value = "resource", access = JsonProperty.Access.WRITE_ONLY)
    private String resource;

    /**
     * A friendly name of the operation.
     */
    @JsonProperty(value = "operation", access = JsonProperty.Access.WRITE_ONLY)
    private String operation;

    /**
     * A friendly description of the operation.
     */
    @JsonProperty(value = "description", access = JsonProperty.Access.WRITE_ONLY)
    private String description;

    /**
     * Get the provider value.
     *
     * @return the provider value
     */
    public String provider() {
        return this.provider;
    }

    /**
     * Get the resource value.
     *
     * @return the resource value
     */
    public String resource() {
        return this.resource;
    }

    /**
     * Get the operation value.
     *
     * @return the operation value
     */
    public String operation() {
        return this.operation;
    }

    /**
     * Get the description value.
     *
     * @return the description value
     */
    public String description() {
        return this.description;
    }

}
