/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

enum AuthorizationStatus {
    Failed,
    Success
};

@JsonIgnoreProperties(ignoreUnknown = true)
class AuthCode {
    AuthCode(final String code) {
        if (code != null) {
            this.status = AuthorizationStatus.Success;
            this.code = code;
        } else {
            this.status = AuthorizationStatus.Failed;
        }
    }

    public AuthorizationStatus getStatus() {
        return status;
    }

    AuthCode(String error, String errorDescription) {
        this.status = AuthorizationStatus.Failed;
        this.error = error != null ? error : "";
        this.errorDescription = errorDescription != null ? errorDescription : "";
    }

    AuthCode(String error, String errorDescription, String errorSubcode) {
        this.status = AuthorizationStatus.Failed;
        this.error = error != null ? error : "";
        this.errorDescription = errorDescription != null ? errorDescription : "";
        this.errorSubcode = errorSubcode != null ? errorSubcode : "";
    }

    private AuthorizationStatus status;

    @JsonProperty("Code")
    private String code = "";

    @JsonProperty("Error")
    private String error = "";

    @JsonProperty("ErrorDescription")
    private String errorDescription;

    private String errorSubcode = "";

    public String getCode() {
        return code;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public String getErrorSubcode() {
        return errorSubcode;
    }

}
