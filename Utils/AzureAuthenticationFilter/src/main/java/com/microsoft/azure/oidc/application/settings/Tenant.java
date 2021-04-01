/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.application.settings;

public interface Tenant {

    String getName();

    boolean equals(Object object);

    int hashCode();

}
