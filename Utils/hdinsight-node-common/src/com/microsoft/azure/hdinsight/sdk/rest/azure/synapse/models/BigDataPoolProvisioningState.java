/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * FIXME: This file is copied from legacy Arcadia API
 *
 * Defines values for SparkComputeProvisioningState.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public enum BigDataPoolProvisioningState {
    /** Enum value Provisioning. */
    PROVISIONING("Provisioning"),

    /** Enum value Succeeded. */
    SUCCEEDED("Succeeded"),

    /** Enum value Failed. */
    FAILED("Failed"),

    /** Enum value Deleting. */
    DELETING("Deleting");

    /** The actual serialized value for a SparkComputeProvisioningState instance. */
    private String value;

    BigDataPoolProvisioningState(String value) {
        this.value = value;
    }

    /**
     * Parses a serialized value to a SparkComputeProvisioningState instance.
     *
     * @param value the serialized value to parse.
     * @return the parsed SparkComputeProvisioningState object, or null if unable to parse.
     */
    @JsonCreator
    public static BigDataPoolProvisioningState fromString(String value) {
        BigDataPoolProvisioningState[] items = BigDataPoolProvisioningState.values();
        for (BigDataPoolProvisioningState item : items) {
            if (item.toString().equalsIgnoreCase(value)) {
                return item;
            }
        }
        return null;
    }

    @JsonValue
    @Override
    public String toString() {
        return this.value;
    }
}
