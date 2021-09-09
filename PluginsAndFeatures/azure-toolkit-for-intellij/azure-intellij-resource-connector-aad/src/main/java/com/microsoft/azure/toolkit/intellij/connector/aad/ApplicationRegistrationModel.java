/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import lombok.Data;

import java.util.List;

/**
 * Manages the data entered in the "Create Azure AD application" form.
 */
@Data
class ApplicationRegistrationModel {
    public static final String DEFAULT_CALLBACK_URL = "http://localhost:8080/secure/aad";

    private String displayName;
    private String clientId;
    private String domain;
    private List<String> callbackUrls;
    private boolean isMultiTenant;
}
