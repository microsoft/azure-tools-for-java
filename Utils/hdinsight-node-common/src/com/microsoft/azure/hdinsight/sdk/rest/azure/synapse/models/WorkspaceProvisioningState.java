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
 * Defines values for WorkspaceProvisioningState.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public enum WorkspaceProvisioningState {
    /** Enum value Provisioning. */
    PROVISIONING("Provisioning"),

    /** Enum value Succeeded. */
    SUCCEEDED("Succeeded"),

    /** Enum value Failed. */
    FAILED("Failed"),

    /** Enum value Deleting. */
    DELETING("Deleting");

    /** The actual serialized value for a WorkspaceProvisioningState instance. */
    private String value;

    WorkspaceProvisioningState(String value) {
        this.value = value;
    }

    /**
     * Parses a serialized value to a WorkspaceProvisioningState instance.
     *
     * @param value the serialized value to parse.
     * @return the parsed WorkspaceProvisioningState object, or null if unable to parse.
     */
    @JsonCreator
    public static WorkspaceProvisioningState fromString(String value) {
        WorkspaceProvisioningState[] items = WorkspaceProvisioningState.values();
        for (WorkspaceProvisioningState item : items) {
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
