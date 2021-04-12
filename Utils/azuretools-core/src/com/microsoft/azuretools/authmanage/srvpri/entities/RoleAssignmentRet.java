/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.entities;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.UUID;

/**
 * Created by vlashch on 8/19/16.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleAssignmentRet {

    @JsonProperty
    public RoleAssignmentRet.Properties properties;
    @JsonProperty
    public String id;
    @JsonProperty
    public String type;
    @JsonProperty
    public String name;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Properties {
        @JsonProperty
        public String roleDefinitionId;
        @JsonProperty
        UUID principalId;
        public String scope;
        @JsonProperty
        public String createdOn;
        @JsonProperty
        public String updatedOn;
        @JsonProperty
        public String createdBy;
        @JsonProperty
        public String updatedBy;
    }
}
