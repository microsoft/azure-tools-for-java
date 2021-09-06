/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.credentialStore.CredentialAttributes;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.common.store.ISecureStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class PasswordStore {
    private static final Map<CredentialAttributes, String> memoStore = new HashMap<>();
    private static final ISecureStore passwordSafe = AzureStoreManager.getInstance().getSecureStore();

    public static void savePassword(String serviceName, String resourceId, String username, char[] password, Password.SaveType passwordSave) {
        if (Password.SaveType.FOREVER == passwordSave) {
            passwordSafe.savePassword(serviceName, resourceId, username, String.valueOf(password));
        } else if (Password.SaveType.UNTIL_RESTART == passwordSave) {
            memoStore.put(new CredentialAttributes(resourceId, username), String.valueOf(password));
        }
    }

    @Nullable
    public static String loadPassword(String serviceName, String resourceId, String username, Password.SaveType passwordSave) {
        if (Password.SaveType.FOREVER == passwordSave) {
            return passwordSafe.loadPassword(serviceName, resourceId, username);
        } else if (Password.SaveType.UNTIL_RESTART == passwordSave) {
            return memoStore.get(new CredentialAttributes(resourceId, username));
        }
        return null;
    }

    public static void forgetPassword(String serviceName, String resourceId, String username) {
        passwordSafe.forgetPassword(serviceName, resourceId, username);
    }

    public static void migratePassword(@Nonnull String oldResourceId, @Nullable String oldUsername,
                                @Nonnull String serviceName, @Nullable String resourceId, @Nullable String userName) {
        passwordSafe.migratePassword(oldResourceId, oldUsername, serviceName, resourceId, userName);
    }
}
