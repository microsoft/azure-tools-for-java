/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.secure;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.securestore.SecureStore;

public class IdeaSecureStore implements SecureStore {
    private IdeaSecureStore() { }

    // Leverage IntelliJ PasswordSafe component
    private PasswordSafe passwordSafe = PasswordSafe.getInstance();

    private static class LazyHolder {
        static final IdeaSecureStore INSTANCE = new IdeaSecureStore();
    }

    public static IdeaSecureStore getInstance() {
        return LazyHolder.INSTANCE;
    }

    @Override
    public void savePassword(@NotNull String serviceName, @NotNull String userName, @Nullable String password) {
        passwordSafe.setPassword(new CredentialAttributes(serviceName, userName), password);
    }

    @Override
    @Nullable
    public String loadPassword(@NotNull String serviceName, @NotNull String userName) {
        return passwordSafe.getPassword(new CredentialAttributes(serviceName, userName));
    }
}
