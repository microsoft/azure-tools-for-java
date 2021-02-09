/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
