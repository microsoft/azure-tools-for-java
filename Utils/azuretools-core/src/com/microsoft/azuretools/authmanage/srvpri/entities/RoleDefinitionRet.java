/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.entities;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * Created by vlashch on 8/18/16.
 */

// incoming

@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleDefinitionRet {
    @JsonProperty
    public List<Value> value;
    @JsonProperty
    public String nextLink;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Value {
        @JsonProperty
        public RoleDefinitionRet.Properties properties;
        @JsonProperty
        public String id;
        @JsonProperty
        public String type;
        @JsonProperty
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Properties {
        @JsonProperty
        public String roleName;
        @JsonProperty
        public String type;
        @JsonProperty
        public String description;
        @JsonProperty
        public List<String> assignableScopes;
        @JsonProperty
        public List<Permissions> permissions;
        @JsonProperty
        public String createdOn;
        @JsonProperty
        public String updatedOn;
        @JsonProperty
        public String createdBy;
        @JsonProperty
        public String updatedBy;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Permissions {
        @JsonProperty
        public List<String> actions;
        @JsonProperty
        public List<String> notActions;

    }
}



