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

package com.microsoft.azure.hdinsight.sdk.rest.azure.projectarcadia.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The virtual network profile of the workspace.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkspaceVirtualNetworkProfile {
    /**
     * The resource id of the virtual network.
     */
    @JsonProperty(value = "virtualNetworkId")
    private String virtualNetworkId;

    /**
     * The resource id of the primary subnet within the specified virtual network.
     */
    @JsonProperty(value = "primarySubnet")
    private String primarySubnet;

    /**
     * The resource id of the compute subnet within the specified virtual network.
     */
    @JsonProperty(value = "computeSubnet")
    private String computeSubnet;

    /**
     * Get the resource id of the virtual network.
     *
     * @return the virtualNetworkId value
     */
    public String virtualNetworkId() {
        return this.virtualNetworkId;
    }

    /**
     * Set the resource id of the virtual network.
     *
     * @param virtualNetworkId the virtualNetworkId value to set
     * @return the WorkspaceVirtualNetworkProfile object itself.
     */
    public WorkspaceVirtualNetworkProfile withVirtualNetworkId(String virtualNetworkId) {
        this.virtualNetworkId = virtualNetworkId;
        return this;
    }

    /**
     * Get the resource id of the primary subnet within the specified virtual network.
     *
     * @return the primarySubnet value
     */
    public String primarySubnet() {
        return this.primarySubnet;
    }

    /**
     * Set the resource id of the primary subnet within the specified virtual network.
     *
     * @param primarySubnet the primarySubnet value to set
     * @return the WorkspaceVirtualNetworkProfile object itself.
     */
    public WorkspaceVirtualNetworkProfile withPrimarySubnet(String primarySubnet) {
        this.primarySubnet = primarySubnet;
        return this;
    }

    /**
     * Get the resource id of the compute subnet within the specified virtual network.
     *
     * @return the computeSubnet value
     */
    public String computeSubnet() {
        return this.computeSubnet;
    }

    /**
     * Set the resource id of the compute subnet within the specified virtual network.
     *
     * @param computeSubnet the computeSubnet value to set
     * @return the WorkspaceVirtualNetworkProfile object itself.
     */
    public WorkspaceVirtualNetworkProfile withComputeSubnet(String computeSubnet) {
        this.computeSubnet = computeSubnet;
        return this;
    }

}
