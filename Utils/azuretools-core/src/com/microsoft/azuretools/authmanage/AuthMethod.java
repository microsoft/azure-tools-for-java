/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage;

import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;
import com.microsoft.azuretools.sdkmanage.AzureCliAzureManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.sdkmanage.ServicePrincipalAzureManager;

public enum AuthMethod {
    AD(new AccessTokenAzureManager.AccessTokenAzureManagerFactory(AdAuthManager.getInstance())),
    DC(new AccessTokenAzureManager.AccessTokenAzureManagerFactory(DCAuthManager.getInstance())),
    SP(new ServicePrincipalAzureManager.ServicePrincipalAzureManagerFactory()),
    AZ(new AzureCliAzureManager.AzureCliAzureManagerFactory()),
    IDENTITY(null);

    private final AzureManagerFactory azureManagerFactory;

    AuthMethod(final AzureManagerFactory azureManagerFactory) {
        this.azureManagerFactory = azureManagerFactory;
    }

    public AzureManager createAzureManager(final AuthMethodDetails authMethodDetails) {
        return this.azureManagerFactory.factory(authMethodDetails);
    }

    public AuthMethodDetails restoreAuth(final AuthMethodDetails authMethodDetails) {
        return this.azureManagerFactory.restore(authMethodDetails);
    }

    public BaseADAuthManager getAdAuthManager() {
        if (azureManagerFactory instanceof AdAuthManagerBuilder) {
            return ((AdAuthManagerBuilder) azureManagerFactory).getInstance();
        }

        throw new IllegalArgumentException("No AD Auth manager instance for authentication method " + this);
    }
}
