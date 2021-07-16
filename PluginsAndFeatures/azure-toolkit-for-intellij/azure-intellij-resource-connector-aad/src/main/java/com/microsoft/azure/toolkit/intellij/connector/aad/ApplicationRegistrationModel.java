/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import lombok.Data;

/**
 * Manages the data entered in the "Create Azure AD application" form.
 */
@Data
class ApplicationRegistrationModel {
    private String displayName;
    private String clientId;
    private String domain;
    private String callbackUrl;
    private boolean isMultiTenant;
}
