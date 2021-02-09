/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage;

import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;

public interface AzureManagerFactory {
    /**
     * Create a new AzureManager implementation instance
     *
     * @param authMethodDetails the authentication method detail parameters
     * @return a new instance
     */
    @Nullable AzureManager factory(final AuthMethodDetails authMethodDetails);

    /**
     * Restore the authentication state from provided details
     *
     * @param authMethodDetails the authentication method detail parameters
     * @return the restored authentication details
     */
    default AuthMethodDetails restore(final AuthMethodDetails authMethodDetails) {
        return authMethodDetails;
    }
}
