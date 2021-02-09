/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.application.settings;

import com.microsoft.azure.oidc.common.id.ID;

public interface ApplicationSettings {

    Tenant getTenant();

    ID getApplicationId();

    Secret getApplicationSecret();

    ID getPrincipalId();

    Secret getPrincipalSecret();

    RedirectURL getRedirectURL();

    Policy getOIDCPolicy();

    boolean equals(Object object);

    int hashCode();

}
