/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Virtual Network Profile.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualNetworkProfile {
    /**
     * Subnet ID used for computes in workspace.
     */
    @JsonProperty(value = "computeSubnetId")
    private String computeSubnetId;

    /**
     * Get subnet ID used for computes in workspace.
     *
     * @return the computeSubnetId value
     */
    public String computeSubnetId() {
        return this.computeSubnetId;
    }

    /**
     * Set subnet ID used for computes in workspace.
     *
     * @param computeSubnetId the computeSubnetId value to set
     * @return the VirtualNetworkProfile object itself.
     */
    public VirtualNetworkProfile withComputeSubnetId(String computeSubnetId) {
        this.computeSubnetId = computeSubnetId;
        return this;
    }

}
