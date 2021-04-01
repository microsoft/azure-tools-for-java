/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.microsoft.azure.toolkit.intellij.link.mysql.PasswordSaveType;
import com.microsoft.azure.toolkit.intellij.link.po.BaseResourcePO;
import com.microsoft.intellij.secure.IdeaSecureStore;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

public abstract class AzureSecurityServiceStorage<T extends BaseResourcePO> extends AzureServiceStorage<T> {

    private final Map<CredentialAttributes, String> memoryCache = new HashMap<>();

    public void savePassword(T service, PasswordSaveType passwordSave, String username, String password) {
        if (PasswordSaveType.FOEVER.equals(passwordSave)) {
            IdeaSecureStore.getInstance().savePassword(service.getId(), username, password);
        } else if (PasswordSaveType.UNTIL_RESTART.equals(passwordSave)) {
            memoryCache.put(new CredentialAttributes(service.getId(), username), password);
        }
    }

    public String loadPassword(T service, PasswordSaveType passwordSave, String username) {
        if (PasswordSaveType.FOEVER.equals(passwordSave)) {
            return IdeaSecureStore.getInstance().loadPassword(service.getId(), username);
        } else if (PasswordSaveType.UNTIL_RESTART.equals(passwordSave)) {
            return memoryCache.get(new CredentialAttributes(service.getId(), username));
        }
        return null;
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    private static class CredentialAttributes {
        private String serviceId;
        private String username;
    }

}
