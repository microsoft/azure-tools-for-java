/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.store;

import com.microsoft.azure.toolkit.ide.common.store.ISecureStore;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class EclipseSecureStore implements ISecureStore {
    ISecurePreferences node;

    public EclipseSecureStore() {
        ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
        node = preferences.node("com.microsoft.azuretools.core");
    }

    @Override
    public void savePassword(@Nonnull String serviceName, @Nullable String key, @Nullable String userName, @Nonnull String password) {
        try {
            node.put(combineKey(serviceName, key, userName), password, true);
            node.flush();
        } catch (StorageException | IOException e) {
            throw new AzureToolkitRuntimeException(e.getMessage(), e);
        }
    }

    private static String combineKey(String service, String key, String username) {
        return Arrays.asList(service, key, username).stream().map(StringUtils::defaultString).collect(Collectors.joining("."));
    }

    @Override
    @Nullable
    public String loadPassword(@Nonnull String serviceName, @Nullable String key, @Nullable String userName) {
        try {
            return node.get(combineKey(serviceName, key, userName), null);
        } catch (StorageException e) {
            throw new AzureToolkitRuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void forgetPassword(@Nonnull String serviceName, @Nullable String key, @Nullable String userName) {
        node.remove(combineKey(serviceName, key, userName));
    }

    @Override
    public void migratePassword(@Nonnull String oldKeyOrServiceName, @Nullable String oldUsername,
                                @Nonnull String serviceName, @Nullable String key, @Nullable String userName) {
    }

    @Nullable
    @Override
    public String getProperty(@Nonnull String service, @Nonnull String key) {
        return loadPassword(service, key, null);
    }

    @Nullable
    @Override
    public String getProperty(@Nonnull String service, @Nonnull String key, @Nullable String defaultValue) {
        return StringUtils.firstNonBlank(loadPassword(service, key, null), defaultValue);
    }

    @Override
    public void setProperty(@Nonnull String service, @Nonnull String key, @Nullable String value) {
        savePassword(service, key, null, value);
    }

}
