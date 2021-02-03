/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Workspace patch details.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkspacePatchInfo {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Properties {
        /**
         * SQL administrator login password.
         */
        @JsonProperty(value = "sqlAdministratorLoginPassword")
        private String sqlAdministratorLoginPassword;

        /**
         * Resource provisioning state.
         */
        @JsonProperty(value = "provisioningState", access = JsonProperty.Access.WRITE_ONLY)
        private String provisioningState;
    }

    @JsonProperty(value = "properties", access = JsonProperty.Access.WRITE_ONLY)
    private Properties properties;

    public Properties properties() {
        return this.properties;
    }

    /**
     * Resource tags.
     */
    @JsonProperty(value = "tags")
    private Map<String, String> tags;

    /**
     * The identity of the workspace.
     */
    @JsonProperty(value = "identity")
    private ManagedIdentity identity;

    /**
     * Get resource tags.
     *
     * @return the tags value
     */
    public Map<String, String> tags() {
        return this.tags;
    }

    /**
     * Set resource tags.
     *
     * @param tags the tags value to set
     * @return the WorkspacePatchInfo object itself.
     */
    public WorkspacePatchInfo withTags(Map<String, String> tags) {
        this.tags = tags;
        return this;
    }

    /**
     * Get the identity of the workspace.
     *
     * @return the identity value
     */
    public ManagedIdentity identity() {
        return this.identity;
    }

    /**
     * Set the identity of the workspace.
     *
     * @param identity the identity value to set
     * @return the WorkspacePatchInfo object itself.
     */
    public WorkspacePatchInfo withIdentity(ManagedIdentity identity) {
        this.identity = identity;
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
     * @return the WorkspacePatchInfo object itself.
     */
    public WorkspacePatchInfo withSqlAdministratorLoginPassword(String sqlAdministratorLoginPassword) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.sqlAdministratorLoginPassword = sqlAdministratorLoginPassword;
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

}
