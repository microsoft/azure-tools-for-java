/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.entities;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.UUID;

/**
 * Created by vlashch on 8/18/16.
 */

// incoming[2]
public class OAuth2PermissionsRet {
    @JsonProperty
    public String adminConsentDescription;
    @JsonProperty
    public String adminConsentDisplayName;
    @JsonProperty
    public UUID id;
    @JsonProperty
    public boolean isEnabled;
    @JsonProperty
    public String lang;
    @JsonProperty
    public String origin;
    @JsonProperty
    public String type;
    @JsonProperty
    public String userConsentDescription;
    @JsonProperty
    public String userConsentDisplayName;
    @JsonProperty
    public String value;
}
