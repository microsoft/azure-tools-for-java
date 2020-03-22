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

