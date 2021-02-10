/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SAS token information.
 */
public class SasTokenInformation {
    /**
     * The access token for the associated Azure Storage Container.
     */
    @JsonProperty(value = "accessToken", access = JsonProperty.Access.WRITE_ONLY)
    private String accessToken;

    /**
     * Get the accessToken value.
     *
     * @return the accessToken value
     */
    public String accessToken() {
        return this.accessToken;
    }

}
