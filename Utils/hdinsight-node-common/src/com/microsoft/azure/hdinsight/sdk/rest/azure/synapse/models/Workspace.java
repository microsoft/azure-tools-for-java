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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A workspace.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Workspace extends TrackedResource {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Properties {
        /**
         * Workspace default data lake storage account details.
         */
        @JsonProperty(value = "defaultDataLakeStorage")
        private DataLakeStorageAccountDetails defaultDataLakeStorage;

        /**
         * SQL administrator login password.
         */
        @JsonProperty(value = "sqlAdministratorLoginPassword")
        private String sqlAdministratorLoginPassword;

        /**
         * Workspace managed resource group.
         */
        @JsonProperty(value = "managedResourceGroupName", access = JsonProperty.Access.WRITE_ONLY)
        private String managedResourceGroupName;

        /**
         * ADLA resource ID.
         */
        @JsonProperty(value = "adlaResourceId")
        private String adlaResourceId;

        /**
         * Resource provisioning state.
         */
        @JsonProperty(value = "provisioningState", access = JsonProperty.Access.WRITE_ONLY)
        private String provisioningState;

        /**
         * Login for workspace SQL active directory administrator.
         */
        @JsonProperty(value = "sqlAdministratorLogin")
        private String sqlAdministratorLogin;

        /**
         * Virtual Network profile.
         */
        @JsonProperty(value = "virtualNetworkProfile")
        private VirtualNetworkProfile virtualNetworkProfile;

        /**
         * Connectivity endpoints.
         */
        @JsonProperty(value = "connectivityEndpoints")
        private Map<String, String> connectivityEndpoints;

        /**
         * Identity of the workspace.
         */
        @JsonProperty(value = "identity")
        private ManagedIdentity identity;
    }

    @JsonProperty(value = "properties", access = JsonProperty.Access.WRITE_ONLY)
    private Properties properties;

    public Properties properties() {
        return this.properties == null ? null : this.properties;
    }

    /**
     * Get workspace default data lake storage account details.
     *
     * @return the defaultDataLakeStorage value
     */
    public DataLakeStorageAccountDetails defaultDataLakeStorage() {
        return this.properties == null ? null : this.properties.defaultDataLakeStorage;
    }

    /**
     * Set workspace default data lake storage account details.
     *
     * @param defaultDataLakeStorage the defaultDataLakeStorage value to set
     * @return the Workspace object itself.
     */
    public Workspace withDefaultDataLakeStorage(DataLakeStorageAccountDetails defaultDataLakeStorage) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.defaultDataLakeStorage = defaultDataLakeStorage;
        return this;
    }

    /**
     * Get sQL administrator login password.
     *
     * @return the sqlAdministratorLoginPassword value
     */
    public String sqlAdministratorLoginPassword() {
        return this.properties == null ? null : this.properties.sqlAdministratorLoginPassword;
    }

    /**
     * Set sQL administrator login password.
     *
     * @param sqlAdministratorLoginPassword the sqlAdministratorLoginPassword value to set
     * @return the Workspace object itself.
     */
    public Workspace withSqlAdministratorLoginPassword(String sqlAdministratorLoginPassword) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.sqlAdministratorLoginPassword = sqlAdministratorLoginPassword;
        return this;
    }

    /**
     * Get workspace managed resource group.
     *
     * @return the managedResourceGroupName value
     */
    public String managedResourceGroupName() {
        return this.properties == null ? null : this.properties.managedResourceGroupName;
    }

    /**
     * Get aDLA resource ID.
     *
     * @return the adlaResourceId value
     */
    public String adlaResourceId() {
        return this.properties == null ? null : this.properties.adlaResourceId;
    }

    /**
     * Set aDLA resource ID.
     *
     * @param adlaResourceId the adlaResourceId value to set
     * @return the Workspace object itself.
     */
    public Workspace withAdlaResourceId(String adlaResourceId) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.adlaResourceId = adlaResourceId;
        return this;
    }

    /**
     * Get resource provisioning state.
     *
     * @return the provisioningState value
     */
    public String provisioningState() {
        return this.properties == null ? null : this.properties.provisioningState;
    }

    /**
     * Get login for workspace SQL active directory administrator.
     *
     * @return the sqlAdministratorLogin value
     */
    public String sqlAdministratorLogin() {
        return this.properties == null ? null : this.properties.sqlAdministratorLogin;
    }

    /**
     * Set login for workspace SQL active directory administrator.
     *
     * @param sqlAdministratorLogin the sqlAdministratorLogin value to set
     * @return the Workspace object itself.
     */
    public Workspace withSqlAdministratorLogin(String sqlAdministratorLogin) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.sqlAdministratorLogin = sqlAdministratorLogin;
        return this;
    }

    /**
     * Get virtual Network profile.
     *
     * @return the virtualNetworkProfile value
     */
    public VirtualNetworkProfile virtualNetworkProfile() {
        return this.properties == null ? null : this.properties.virtualNetworkProfile;
    }

    /**
     * Set virtual Network profile.
     *
     * @param virtualNetworkProfile the virtualNetworkProfile value to set
     * @return the Workspace object itself.
     */
    public Workspace withVirtualNetworkProfile(VirtualNetworkProfile virtualNetworkProfile) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.virtualNetworkProfile = virtualNetworkProfile;
        return this;
    }

    /**
     * Get connectivity endpoints.
     *
     * @return the connectivityEndpoints value
     */
    public Map<String, String> connectivityEndpoints() {
        return this.properties == null ? null : this.properties.connectivityEndpoints;
    }

    /**
     * Set connectivity endpoints.
     *
     * @param connectivityEndpoints the connectivityEndpoints value to set
     * @return the Workspace object itself.
     */
    public Workspace withConnectivityEndpoints(Map<String, String> connectivityEndpoints) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.connectivityEndpoints = connectivityEndpoints;
        return this;
    }

    /**
     * Get identity of the workspace.
     *
     * @return the identity value
     */
    public ManagedIdentity identity() {
        return this.properties == null ? null : this.properties.identity;
    }

    /**
     * Set identity of the workspace.
     *
     * @param identity the identity value to set
     * @return the Workspace object itself.
     */
    public Workspace withIdentity(ManagedIdentity identity) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.identity = identity;
        return this;
    }

}
