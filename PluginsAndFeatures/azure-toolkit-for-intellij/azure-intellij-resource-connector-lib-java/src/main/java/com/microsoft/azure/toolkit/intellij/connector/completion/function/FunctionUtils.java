/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.completion.function;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.connector.function.FunctionSupported;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class FunctionUtils {

    @Nullable
    public static String getConnectionValueFromAnnotation(@Nonnull final PsiAnnotation annotation) {
        final PsiAnnotationMemberValue connectionValue = Optional.ofNullable(annotation.findAttributeValue("connection"))
                .orElseGet(() -> annotation.findAttributeValue("value"));
        return Objects.isNull(connectionValue) ? null : connectionValue.getText().replace("\"", "");
    }

    @Nullable
    public static Connection<?, ?> getConnectionFromAnnotation(@Nonnull final PsiAnnotation annotation) {
        final String connectionValueFromAnnotation = getConnectionValueFromAnnotation(annotation);
        final Module module = ModuleUtil.findModuleForPsiElement(annotation);
        return getConnectionWithEnvPrefix(connectionValueFromAnnotation, module);
    }

    @Nullable
    public static Connection<?, ?> getConnectionWithEnvPrefix(@Nullable final String envPrefix, @Nullable Module module) {
        final Profile profile = Optional.ofNullable(module).map(AzureModule::from).map(AzureModule::getDefaultProfile).orElse(null);
        return Objects.isNull(profile) ? null : profile.getConnections().stream()
                .filter(c -> c.getDefinition().getResourceDefinition() instanceof FunctionSupported<?>)
                .filter(c -> Objects.nonNull(envPrefix) && StringUtils.equals(((FunctionSupported<?>) c.getDefinition().getResourceDefinition()).getFunctionProperty(c), envPrefix))
                .findFirst().orElse(null);
    }
}
