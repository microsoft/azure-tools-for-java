/**
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
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
