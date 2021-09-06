/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.store;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface for secure store operations
 */
public interface ISecureStore {
    /**
     * Load password or credential from the secure store
     *
     * @param serviceName the unique service name
     * @param key the key for the credential
     * @param userName the user name for the credential, may be null, if no username is suitable
     * @return the password stored for serviceName and userName combination, null for not found
     */
    @Nullable String loadPassword(@Nonnull String serviceName, @Nullable String key, @Nullable String userName);

    /**
     * Save password or credential into the secure store
     *
     * @param serviceName the unique service name
     * @param key the key for the credential, may be null, if no key is suitable
     * @param userName the user name for the credential, may be null, if no username is suitable
     * @param password the password for serviceName and userName combination to store, null for cleaning the saved one
     */
    void savePassword(@Nonnull String serviceName, @Nullable String key, @Nullable String userName, @Nonnull String password);

    /**
     * Remove password or credential from the secure store
     *
     * @param serviceName the unique service name
     * @param key the key for the credential, may be null, if no key is suitable
     * @param userName the user name or key for the credential
     */
    void forgetPassword(@Nonnull String serviceName, @Nullable String key, @Nullable String userName);

    /**
     * Migration password for old style to new style
     *
     * @param oldKeyOrServiceName the previous service name or key
     * @param oldUsername the previous user name or key for the credential
     * @param serviceName the unique service name
     * @param key the key for the credential, may be null, if no key is suitable
     * @param userName the user name or key for the credential
     */
    void migratePassword(@Nonnull String oldKeyOrServiceName, @Nullable String oldUsername,
                         @Nonnull String serviceName, @Nullable String key, @Nullable String userName);
}
