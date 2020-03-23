/*
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
