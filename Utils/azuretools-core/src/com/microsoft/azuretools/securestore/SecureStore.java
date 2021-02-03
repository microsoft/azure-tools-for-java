/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.securestore;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

/**
 * Interface for secure store operations
 */
public interface SecureStore {
    /**
     * Load password or credential from the secure store
     *
     * @param serviceName the unique service name
     * @param userName the user name or key for the credential
     * @return the password stored for serviceName and userName combination, null for not found
     */
    @Nullable
    String loadPassword(@NotNull String serviceName, @NotNull String userName);

    /**
     * Save password or credential into the secure store
     *
     * @param serviceName the unique service name
     * @param userName the user name or key for the credential
     * @param password the password for serviceName and userName combination to store, null for cleaning the saved one
     */
    void savePassword(@NotNull String serviceName, @NotNull String userName, @Nullable String password);
}
