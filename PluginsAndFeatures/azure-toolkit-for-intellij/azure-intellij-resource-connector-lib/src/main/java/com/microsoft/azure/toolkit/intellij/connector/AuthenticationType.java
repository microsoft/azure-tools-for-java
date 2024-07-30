/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthenticationType {
    SYSTEM_ASSIGNED_MANAGED_IDENTITY("Managed Identity (System Assigned)"),
    USER_ASSIGNED_MANAGED_IDENTITY("Managed Identity (User Assigned)"),
    CONNECTION_STRING("Connection String");

    private String displayName;
}
