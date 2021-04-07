/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class PasswordStore {
    private static final PasswordSafe passwordSafe = PasswordSafe.getInstance();

    private static final Map<CredentialAttributes, String> memoStore = new HashMap<>();

    public static void savePassword(String resourceId, String username, char[] password, Password.SaveType passwordSave) {
        if (Password.SaveType.FOREVER == passwordSave) {
            passwordSafe.setPassword(new CredentialAttributes(resourceId, username), String.valueOf(password));
        } else if (Password.SaveType.UNTIL_RESTART == passwordSave) {
            memoStore.put(new CredentialAttributes(resourceId, username), String.valueOf(password));
        }
    }

    @Nullable
    public static String loadPassword(String resourceId, String username, Password.SaveType passwordSave) {
        if (Password.SaveType.FOREVER == passwordSave) {
            passwordSafe.getPassword(new CredentialAttributes(resourceId, username));
        } else if (Password.SaveType.UNTIL_RESTART == passwordSave) {
            return memoStore.get(new CredentialAttributes(resourceId, username));
        }
        return null;
    }
}
