/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Annotator for validations of resource connection variables in String
 * Mostly it will check following items:
 * 1. whether connected resource is valid (deleted/broken)
 * 2. whether there are string follow the same pattern but did not appears in environment variables,
 * which means the correspond connection may be deleted
 */
public abstract class AbstractResourceConnectionAnnotator implements Annotator {
    @Override
    public void annotate(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        if (!(isAzureFacetEnabled(element) && shouldAccept(element))) {
            return;
        }
        final Connection<?, ?> connection = getConnectionForPsiElement(element);
        if (Objects.isNull(connection)) {
            validateNamePattern(element, holder);
        } else {
            validateConnectionResource(element, holder, connection);
        }
    }

    public static boolean isAzureFacetEnabled(@Nonnull PsiElement element) {
        final Module module = ModuleUtil.findModuleForPsiElement(element);
        return Optional.ofNullable(module).map(AzureModule::from).map(AzureModule::hasAzureFacet).orElse(false);
    }

    protected abstract boolean shouldAccept(@Nonnull final PsiElement element);

    protected abstract String extractVariableValue(@Nonnull final PsiElement element);

    @Nullable
    protected Connection<?, ?> getConnectionForPsiElement(@Nonnull final PsiElement element) {
        final Module module = ModuleUtil.findModuleForPsiElement(element);
        return Utils.getConnectionWithEnvironmentVariable(module, extractVariableValue(element));
    }

    private void validateNamePattern(@Nonnull final PsiElement element, @Nonnull final AnnotationHolder holder) {
        final Profile profile = Optional.ofNullable(ModuleUtil.findModuleForPsiElement(element))
                .map(AzureModule::from)
                .map(AzureModule::getDefaultProfile).orElse(null);
        if (Objects.isNull(profile)) {
            return;
        }
        final String text = extractVariableValue(element);
        //noinspection ReturnOfNull
        final AzureServiceResource.Definition<?> definition = ConnectionManager.getDefinitions().stream()
                .map(d -> d.getResourceDefinition() instanceof AzureServiceResource.Definition ?
                        (AzureServiceResource.Definition<?>) d.getResourceDefinition() : null)
                .filter(Objects::nonNull)
                .filter(d -> d.getEnvironmentVariablesKey().stream().anyMatch(k -> StringUtils.endsWith(text, StringUtils.removeStart(k, Connection.ENV_PREFIX))))
                .findFirst().orElse(null);
        if (Objects.nonNull(definition)) {
            final AnnotationBuilder builder = holder.newAnnotation(HighlightSeverity.WARNING, String.format("Could not find resource connection with variable %s", text))
                    .range(element.getTextRange())
                    .highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
            final String suffix = definition.getEnvironmentVariablesKey().stream()
                    .map(key -> StringUtils.removeStart(key, Connection.ENV_PREFIX))
                    .filter(k -> StringUtils.endsWith(text, k))
                    .findFirst().orElse(null);
            final List<String> values = profile.getConnections().stream()
                    .filter(c -> Objects.equals(c.getResource().getDefinition(), definition))
                    .flatMap(c -> profile.getGeneratedEnvironmentVariables(c).stream())
                    .map(Pair::getKey)
                    .filter(key -> StringUtils.isNotBlank(suffix) && StringUtils.endsWith(key, suffix))
                    .toList();
            //noinspection ResultOfMethodCallIgnored
            values.stream()
                    .map(property -> new ChangeEnvironmentVariableFix(text, property, SmartPointerManager.createPointer(element)))
                    .forEach(builder::withFix);
            builder.create();
        }
    }

    private void validateConnectionResource(@Nonnull final PsiElement element, @Nonnull final AnnotationHolder holder,
                                            @Nonnull final Connection<?, ?> connection) {
        final Object data = connection.getResource().getData();
        if (!Azure.az(AzureAccount.class).isLoggedIn() && data instanceof AbstractAzResource<?,?,?> r && !r.isMocked()) {
            AnnotationFixes.createSignInAnnotation(element, holder);
            return;
        }
        if (!connection.isValidConnection()) {
            holder.newAnnotation(HighlightSeverity.WARNING, String.format("Connection '%s' is not valid", connection.getEnvPrefix()))
                    .range(element.getTextRange())
                    .highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                    .withFix(new EditConnectionFix(connection))
                    .create();
        }
    }
}
