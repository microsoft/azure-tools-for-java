/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Error details.
 * Contains details when the response code indicates an error.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorContract {
    /**
     * The error details.
     */
    @JsonProperty(value = "error")
    private ErrorResponse error;

    /**
     * Get the error details.
     *
     * @return the error value
     */
    public ErrorResponse error() {
        return this.error;
    }

    /**
     * Set the error details.
     *
     * @param error the error value to set
     * @return the ErrorContract object itself.
     */
    public ErrorContract withError(ErrorResponse error) {
        this.error = error;
        return this;
    }

}
