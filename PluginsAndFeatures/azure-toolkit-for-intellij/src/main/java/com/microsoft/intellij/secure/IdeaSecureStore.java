/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.secure;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.microsoft.azure.toolkit.ide.common.store.ISecureStore;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.intellij.credentialStore.CredentialAttributesKt.generateServiceName;

public class IdeaSecureStore implements ISecureStore {
    private IdeaSecureStore() {
    }

    // Leverage IntelliJ PasswordSafe component
    private PasswordSafe passwordSafe = PasswordSafe.getInstance();

    private static class LazyHolder {
        static final IdeaSecureStore INSTANCE = new IdeaSecureStore();
    }

    public static IdeaSecureStore getInstance() {
        return LazyHolder.INSTANCE;
    }

    @Override
    public void savePassword(@Nonnull String serviceName, @Nullable String key, @Nullable String userName, @Nonnull String password) {
        passwordSafe.setPassword(makeKey(serviceName, key, userName), password);
    }

    @Override
    @Nullable
    public String loadPassword(@Nonnull String serviceName, @Nullable String key, @Nullable String userName) {
        return passwordSafe.getPassword(makeKey(serviceName, key, userName));
    }

    @Override
    public void forgetPassword(@Nonnull String serviceName, @Nullable String key, @Nullable String userName) {
        CredentialAttributes oldKey = StringUtils.isNotBlank(userName) ? new CredentialAttributes(key, userName) :
            new CredentialAttributes(key);
        passwordSafe.setPassword(oldKey, null);
        passwordSafe.setPassword(makeKey(serviceName, key, userName), null);
    }

    @Override
    public void migratePassword(@Nonnull String oldKeyOrServiceName, @Nullable String oldUsername,
                                @Nonnull String serviceName, @Nullable String key, @Nullable String userName) {
        CredentialAttributes oldKey = StringUtils.isNotBlank(oldUsername) ? new CredentialAttributes(oldKeyOrServiceName, userName) :
            new CredentialAttributes(oldKeyOrServiceName);
        CredentialAttributes newKey = makeKey(serviceName, key, userName);
        if (StringUtils.isBlank(passwordSafe.getPassword(newKey))) {
            passwordSafe.setPassword(newKey, passwordSafe.getPassword(oldKey));
        }
        passwordSafe.setPassword(oldKey, null);
    }

    @Nonnull
    private static CredentialAttributes makeKey(String serviceName, @Nullable String key, @Nullable String userName) {
        String serverNameWithPrefix = serviceName;
        if (!StringUtils.contains(serviceName, "Azure IntelliJ Plugin")) {
            serverNameWithPrefix = StringUtils.join("Azure IntelliJ Plugin | " + serviceName);
        }
        if (StringUtils.isAllBlank(key, userName)) {
            return new CredentialAttributes(serverNameWithPrefix);
        } else if (StringUtils.isNoneBlank(key, userName)) {
            return new CredentialAttributes(generateServiceName(serverNameWithPrefix, key), userName);
        } else if (StringUtils.isNotBlank(key)) {
            return new CredentialAttributes(generateServiceName(serverNameWithPrefix, key));
        }
        return new CredentialAttributes(serverNameWithPrefix, userName);
    }
}
