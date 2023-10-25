/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.completion.resource.function;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.storage.connection.StorageAccountResourceDefinition;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class FunctionUtils {

    public static StorageAccount getBindingStorageAccount(@Nonnull final PsiAnnotation annotation) {
        return Optional.ofNullable(getBindingConnection(annotation))
                .map(c -> (StorageAccount) c.getResource().getData())
                .orElse(null);
    }

    @Nullable
    public static Connection<?, ?> getBindingConnection(@Nonnull final PsiAnnotation annotation) {
        final Module module = ModuleUtil.findModuleForPsiElement(annotation);
        final Profile profile = Optional.ofNullable(module).map(AzureModule::from).map(AzureModule::getDefaultProfile).orElse(null);
        final PsiAnnotationMemberValue connectionValue = Optional.ofNullable(annotation.findAttributeValue("connection"))
                .orElseGet(() -> annotation.findAttributeValue("value"));
        if (Objects.isNull(profile) || Objects.isNull(connectionValue)) {
            return null;
        }
        final String connection = connectionValue.getText().replace("\"", "");
        return profile.getConnections().stream()
                .filter(c -> Objects.equals(c.getResource().getDefinition(), StorageAccountResourceDefinition.INSTANCE))
                .filter(c -> StringUtils.equals(((StorageAccountResourceDefinition) c.getResource().getDefinition()).getFunctionProperty(c), connection))
                .findFirst().orElse(null);
    }
}
