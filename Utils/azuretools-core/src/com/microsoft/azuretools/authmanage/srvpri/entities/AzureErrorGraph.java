/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.entities;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Created by vlashch on 8/22/16.
 */


@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureErrorGraph {

    @JsonProperty("odata.error")
    public Error error;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Error {
        @JsonProperty
        public String code;
        @JsonProperty
        public Message message;
        @JsonProperty
        public String date;
        @JsonProperty
        public String requestId;
        @JsonProperty
        public Value[] values;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Message {
            @JsonProperty
            public String lang;
            @JsonProperty
            public String value;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Value {
            @JsonProperty
            public String item;
            @JsonProperty
            public String value;
        }
    }
}

