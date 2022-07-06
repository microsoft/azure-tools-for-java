/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethod;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Created by shch on 10/8/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthMethodDetails {

    @JsonProperty
    @Setter
    @Getter
    private String accountEmail;

    @JsonProperty
    @Setter
    @Getter
    @Deprecated
    private String tenantId;

    @JsonProperty
    @Setter
    @Getter
    private String clientId;

    @Deprecated
    @JsonProperty
    @Setter
    @Getter
    private String credFilePath;

    @JsonProperty
    @Setter
    @Getter
    private String certificate;

    @JsonProperty
    @Setter
    @Getter
    private AuthMethod authMethod;

    @JsonProperty
    @Setter
    @Getter
    private String azureEnv;

    /**
     * new attributes: auth type
     */
    @JsonProperty
    @Setter
    @Getter
    private AuthType authType;

    @JsonProperty
    @Setter
    @Getter
    private List<String> tenantIds;

    @JsonProperty
    @Setter
    @Getter
    private List<Subscription> subscriptions;

    // for jackson json
    public AuthMethodDetails() {
    }

    @Override
    public String toString() {
        if (getAuthMethod() == AuthMethod.IDENTITY) {
            return String.format("{ accountEmail: %s, credFilePath: %s, authType: %s, azureEnv: %s }",
                getAccountEmail(), getCredFilePath(), getAuthType(), getAzureEnv());
        }
        return String.format("{ accountEmail: %s, credFilePath: %s, authMethod: %s, azureEnv: %s }",
            getAccountEmail(), getCredFilePath(), getAuthMethod(), getAzureEnv());
    }
}
