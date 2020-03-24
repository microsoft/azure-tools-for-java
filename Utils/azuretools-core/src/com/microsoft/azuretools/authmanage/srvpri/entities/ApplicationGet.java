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
import java.util.UUID;

/**
 * Created by vlashch on 4/28/2017.
 */
// inconming
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationGet {

    @JsonProperty("odata.metadata")
    public String odata_metadata;

    @JsonProperty("value")
    public List<Value> value;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Value {
        @JsonProperty("odata.type")
        public String odata_type;

        @JsonProperty
        public String objectType;

        @JsonProperty
        public UUID objectId;

        //"deletionTimestamp":null
        //"addIns":[]

        @JsonProperty
        public String deletionTimestamp;

        //"appRoles":[],

        @JsonProperty
        public UUID appId;

        //appRoles":[]
        //"availableToOtherTenants":false,

        @JsonProperty
        public String displayName;

        //"errorUrl":null,
        //"groupMembershipClaims":null,

        @JsonProperty
        public String homepage;

        @JsonProperty
        public List<String> identifierUris;

        //"keyCredentials":[]
        //"knownClientApplications":[],
        //"logoutUrl":null,
        //"oauth2AllowImplicitFlow":false,
        //"oauth2AllowUrlPathMatching":false,

        @JsonProperty
        public List<Oauth2Permission> oauth2Permissions;

        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class Oauth2Permission {
            @JsonProperty
            public String adminConsentDescription;
            @JsonProperty
            public String adminConsentDisplayName;
            @JsonProperty
            public UUID id;
            @JsonProperty
            public boolean isEnabled;
            @JsonProperty
            public String type;
            @JsonProperty
            public String userConsentDescription;
            @JsonProperty
            public String userConsentDisplayName;
            @JsonProperty
            public String value;
        }

        //"oauth2RequirePostResponse":false,
        //"optionalClaims":null,
    }

    @JsonProperty
    public List<PasswordCredential> passwordCredentials;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PasswordCredential {
        @JsonProperty
        public String customKeyIdentifier;
        @JsonProperty
        public String endDate;
        @JsonProperty
        public String keyId;
        @JsonProperty
        public String startDate;
        @JsonProperty
        public String value;

    }

//     "publicClient":null,
//     "recordConsentConditions":null,
//     "replyUrls":[],
//     "requiredResourceAccess":[],
//     "samlMetadataUrl":null

}
