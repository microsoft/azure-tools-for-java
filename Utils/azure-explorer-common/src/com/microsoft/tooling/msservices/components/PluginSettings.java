/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.components;

public class PluginSettings {
    private String clientId;
    private String tenantName;
    private String redirectUri;
    private String azureServiceManagementUri;
    private String graphApiUri;
    private String adAuthority;
    private String graphApiVersion;
    private String pluginVersion;

    public String getClientId() {
        return clientId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getAzureServiceManagementUri() {
        return azureServiceManagementUri;
    }

    public String getGraphApiUri() {
        return graphApiUri;
    }

    public String getAdAuthority() {
        return adAuthority;
    }

    public String getGraphApiVersion() {
        return graphApiVersion;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }
}
