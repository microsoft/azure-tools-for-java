/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.models;

import com.microsoft.azuretools.authmanage.AuthMethod;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Created by shch on 10/8/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthMethodDetails {

    @JsonProperty
    private String accountEmail;

    @JsonProperty
    private String credFilePath;

    @JsonProperty
    private AuthMethod authMethod;

    @JsonProperty
    private String azureEnv;

    // for jackson json
    public AuthMethodDetails() {
    }

    public AuthMethod getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(AuthMethod authMethod) {
        this.authMethod = authMethod;
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public void setAccountEmail(String accountEmail) {
        this.accountEmail = accountEmail;
    }

    public String getCredFilePath() {
        return credFilePath;
    }

    public void setCredFilePath(String credFilePath) {
        this.credFilePath = credFilePath;
    }

    public String getAzureEnv() {
        return azureEnv;
    }

    public void setAzureEnv(String azureEnv) {
        this.azureEnv = azureEnv;
    }

    @Override
    public String toString() {
        return String.format("{ accountEmail: %s, credFilePath: %s, authMethod: %s, azureEnv: %s }",
                getAccountEmail(), getCredFilePath(), getAuthMethod(), getAzureEnv());
    }
}
