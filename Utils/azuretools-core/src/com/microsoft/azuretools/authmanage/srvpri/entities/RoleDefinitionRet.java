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



