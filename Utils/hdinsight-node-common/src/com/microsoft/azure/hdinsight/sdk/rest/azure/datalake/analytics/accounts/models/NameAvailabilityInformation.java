/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Lake Analytics account name availability result information.
 */
public class NameAvailabilityInformation {
    /**
     * The Boolean value of true or false to indicate whether the Data Lake
     * Analytics account name is available or not.
     */
    @JsonProperty(value = "nameAvailable", access = JsonProperty.Access.WRITE_ONLY)
    private Boolean nameAvailable;

    /**
     * The reason why the Data Lake Analytics account name is not available, if
     * nameAvailable is false.
     */
    @JsonProperty(value = "reason", access = JsonProperty.Access.WRITE_ONLY)
    private String reason;

    /**
     * The message describing why the Data Lake Analytics account name is not
     * available, if nameAvailable is false.
     */
    @JsonProperty(value = "message", access = JsonProperty.Access.WRITE_ONLY)
    private String message;

    /**
     * Get the nameAvailable value.
     *
     * @return the nameAvailable value
     */
    public Boolean nameAvailable() {
        return this.nameAvailable;
    }

    /**
     * Get the reason value.
     *
     * @return the reason value
     */
    public String reason() {
        return this.reason;
    }

    /**
     * Get the message value.
     *
     * @return the message value
     */
    public String message() {
        return this.message;
    }

}
