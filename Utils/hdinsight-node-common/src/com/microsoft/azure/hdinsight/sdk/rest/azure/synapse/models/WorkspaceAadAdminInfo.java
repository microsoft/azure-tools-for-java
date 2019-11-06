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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.rest.serializer.JsonFlatten;

/**
 * Workspace active directory administrator.
 */
@JsonFlatten
public class WorkspaceAadAdminInfo extends ProxyResource {
    /**
     * Tenant ID of the workspace active directory administrator.
     */
    @JsonProperty(value = "properties.tenantId")
    private String tenantId;

    /**
     * Login of the workspace active directory administrator.
     */
    @JsonProperty(value = "properties.login")
    private String login;

    /**
     * Workspace active directory administrator type.
     */
    @JsonProperty(value = "properties.administratorType")
    private String administratorType;

    /**
     * Object ID of the workspace active directory administrator.
     */
    @JsonProperty(value = "properties.sid")
    private String sid;

    /**
     * Get tenant ID of the workspace active directory administrator.
     *
     * @return the tenantId value
     */
    public String tenantId() {
        return this.tenantId;
    }

    /**
     * Set tenant ID of the workspace active directory administrator.
     *
     * @param tenantId the tenantId value to set
     * @return the WorkspaceAadAdminInfo object itself.
     */
    public WorkspaceAadAdminInfo withTenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    /**
     * Get login of the workspace active directory administrator.
     *
     * @return the login value
     */
    public String login() {
        return this.login;
    }

    /**
     * Set login of the workspace active directory administrator.
     *
     * @param login the login value to set
     * @return the WorkspaceAadAdminInfo object itself.
     */
    public WorkspaceAadAdminInfo withLogin(String login) {
        this.login = login;
        return this;
    }

    /**
     * Get workspace active directory administrator type.
     *
     * @return the administratorType value
     */
    public String administratorType() {
        return this.administratorType;
    }

    /**
     * Set workspace active directory administrator type.
     *
     * @param administratorType the administratorType value to set
     * @return the WorkspaceAadAdminInfo object itself.
     */
    public WorkspaceAadAdminInfo withAdministratorType(String administratorType) {
        this.administratorType = administratorType;
        return this;
    }

    /**
     * Get object ID of the workspace active directory administrator.
     *
     * @return the sid value
     */
    public String sid() {
        return this.sid;
    }

    /**
     * Set object ID of the workspace active directory administrator.
     *
     * @param sid the sid value to set
     * @return the WorkspaceAadAdminInfo object itself.
     */
    public WorkspaceAadAdminInfo withSid(String sid) {
        this.sid = sid;
        return this;
    }

}
