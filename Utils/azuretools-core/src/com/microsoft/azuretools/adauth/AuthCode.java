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