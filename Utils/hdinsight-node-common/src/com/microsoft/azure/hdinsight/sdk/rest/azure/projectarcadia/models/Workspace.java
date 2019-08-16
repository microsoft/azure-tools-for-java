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

@JsonIgnoreProperties(ignoreUnknown = true)
public class Workspace extends TrackedResource {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Properties {
        /**
         * The workspace provisioning state. Possible values include: 'Provisioning', 'Succeeded', 'Failed', 'Deleting'.
         */
        @JsonProperty(value = "provisioningState", access = JsonProperty.Access.WRITE_ONLY)
        private WorkspaceProvisioningState provisioningState;

        /**
         * The virtual network properties.
         */
        @JsonProperty(value = "virtualNetworkProfile")
        private WorkspaceVirtualNetworkProfile virtualNetworkProfile;

        /**
         * Workspace connectivity endpoints.
         */
        @JsonProperty(value = "connectivityEndpoints", access = JsonProperty.Access.WRITE_ONLY)
        private ConnectivityEndpoints connectivityEndpoints;
    }

    @JsonProperty(value = "properties", access = JsonProperty.Access.WRITE_ONLY)
    private Properties properties;

    public Properties properties() {
        return this.properties;
    }

    /**
     * Get the workspace provisioning state. Possible values include: 'Provisioning', 'Succeeded', 'Failed', 'Deleting'.
     *
     * @return the provisioningState value
     */
    public WorkspaceProvisioningState provisioningState() {
        return this.properties == null ? null : this.properties.provisioningState;
    }

    /**
     * Get the virtual network properties.
     *
     * @return the virtualNetworkProfile value
     */
    public WorkspaceVirtualNetworkProfile virtualNetworkProfile() {
        return this.properties == null ? null : this.properties.virtualNetworkProfile;
    }

    /**
     * Set the virtual network properties.
     *
     * @param virtualNetworkProfile the virtualNetworkProfile value to set
     * @return the Workspace object itself.
     */
    public Workspace withVirtualNetworkProfile(WorkspaceVirtualNetworkProfile virtualNetworkProfile) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.virtualNetworkProfile = virtualNetworkProfile;
        return this;
    }

    /**
     * Get workspace connectivity endpoints.
     *
     * @return the connectivityEndpoints value
     */
    public ConnectivityEndpoints connectivityEndpoints() {
        return this.properties == null ? null : this.properties.connectivityEndpoints;
    }
}
