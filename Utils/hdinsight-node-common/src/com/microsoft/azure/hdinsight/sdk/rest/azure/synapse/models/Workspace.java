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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.rest.serializer.JsonFlatten;

/**
 * A workspace.
 */
@JsonFlatten
public class Workspace extends TrackedResource {
    /**
     * Workspace default data lake storage account details.
     */
    @JsonProperty(value = "properties.defaultDataLakeStorage")
    private DataLakeStorageAccountDetails defaultDataLakeStorage;

    /**
     * SQL administrator login password.
     */
    @JsonProperty(value = "properties.sqlAdministratorLoginPassword")
    private String sqlAdministratorLoginPassword;

    /**
     * Workspace managed resource group.
     */
    @JsonProperty(value = "properties.managedResourceGroupName", access = JsonProperty.Access.WRITE_ONLY)
    private String managedResourceGroupName;

    /**
     * ADLA resource ID.
     */
    @JsonProperty(value = "properties.adlaResourceId")
    private String adlaResourceId;

    /**
     * Resource provisioning state.
     */
    @JsonProperty(value = "properties.provisioningState", access = JsonProperty.Access.WRITE_ONLY)
    private String provisioningState;

    /**
     * Login for workspace SQL active directory administrator.
     */
    @JsonProperty(value = "properties.sqlAdministratorLogin")
    private String sqlAdministratorLogin;

    /**
     * Virtual Network profile.
     */
    @JsonProperty(value = "properties.virtualNetworkProfile")
    private VirtualNetworkProfile virtualNetworkProfile;

    /**
     * Connectivity endpoints.
     */
    @JsonProperty(value = "properties.connectivityEndpoints")
    private Map<String, String> connectivityEndpoints;

    /**
     * Identity of the workspace.
     */
    @JsonProperty(value = "identity")
    private ManagedIdentity identity;

    /**
     * Get workspace default data lake storage account details.
     *
     * @return the defaultDataLakeStorage value
     */
    public DataLakeStorageAccountDetails defaultDataLakeStorage() {
        return this.defaultDataLakeStorage;
    }

    /**
     * Set workspace default data lake storage account details.
     *
     * @param defaultDataLakeStorage the defaultDataLakeStorage value to set
     * @return the Workspace object itself.
     */
    public Workspace withDefaultDataLakeStorage(DataLakeStorageAccountDetails defaultDataLakeStorage) {
        this.defaultDataLakeStorage = defaultDataLakeStorage;
        return this;
    }

    /**
     * Get sQL administrator login password.
     *
     * @return the sqlAdministratorLoginPassword value
     */
    public String sqlAdministratorLoginPassword() {
        return this.sqlAdministratorLoginPassword;
    }

    /**
     * Set sQL administrator login password.
     *
     * @param sqlAdministratorLoginPassword the sqlAdministratorLoginPassword value to set
     * @return the Workspace object itself.
     */
    public Workspace withSqlAdministratorLoginPassword(String sqlAdministratorLoginPassword) {
        this.sqlAdministratorLoginPassword = sqlAdministratorLoginPassword;
        return this;
    }

    /**
     * Get workspace managed resource group.
     *
     * @return the managedResourceGroupName value
     */
    public String managedResourceGroupName() {
        return this.managedResourceGroupName;
    }

    /**
     * Get aDLA resource ID.
     *
     * @return the adlaResourceId value
     */
    public String adlaResourceId() {
        return this.adlaResourceId;
    }

    /**
     * Set aDLA resource ID.
     *
     * @param adlaResourceId the adlaResourceId value to set
     * @return the Workspace object itself.
     */
    public Workspace withAdlaResourceId(String adlaResourceId) {
        this.adlaResourceId = adlaResourceId;
        return this;
    }

    /**
     * Get resource provisioning state.
     *
     * @return the provisioningState value
     */
    public String provisioningState() {
        return this.provisioningState;
    }

    /**
     * Get login for workspace SQL active directory administrator.
     *
     * @return the sqlAdministratorLogin value
     */
    public String sqlAdministratorLogin() {
        return this.sqlAdministratorLogin;
    }

    /**
     * Set login for workspace SQL active directory administrator.
     *
     * @param sqlAdministratorLogin the sqlAdministratorLogin value to set
     * @return the Workspace object itself.
     */
    public Workspace withSqlAdministratorLogin(String sqlAdministratorLogin) {
        this.sqlAdministratorLogin = sqlAdministratorLogin;
        return this;
    }

    /**
     * Get virtual Network profile.
     *
     * @return the virtualNetworkProfile value
     */
    public VirtualNetworkProfile virtualNetworkProfile() {
        return this.virtualNetworkProfile;
    }

    /**
     * Set virtual Network profile.
     *
     * @param virtualNetworkProfile the virtualNetworkProfile value to set
     * @return the Workspace object itself.
     */
    public Workspace withVirtualNetworkProfile(VirtualNetworkProfile virtualNetworkProfile) {
        this.virtualNetworkProfile = virtualNetworkProfile;
        return this;
    }

    /**
     * Get connectivity endpoints.
     *
     * @return the connectivityEndpoints value
     */
    public Map<String, String> connectivityEndpoints() {
        return this.connectivityEndpoints;
    }

    /**
     * Set connectivity endpoints.
     *
     * @param connectivityEndpoints the connectivityEndpoints value to set
     * @return the Workspace object itself.
     */
    public Workspace withConnectivityEndpoints(Map<String, String> connectivityEndpoints) {
        this.connectivityEndpoints = connectivityEndpoints;
        return this;
    }

    /**
     * Get identity of the workspace.
     *
     * @return the identity value
     */
    public ManagedIdentity identity() {
        return this.identity;
    }

    /**
     * Set identity of the workspace.
     *
     * @param identity the identity value to set
     * @return the Workspace object itself.
     */
    public Workspace withIdentity(ManagedIdentity identity) {
        this.identity = identity;
        return this;
    }

}
