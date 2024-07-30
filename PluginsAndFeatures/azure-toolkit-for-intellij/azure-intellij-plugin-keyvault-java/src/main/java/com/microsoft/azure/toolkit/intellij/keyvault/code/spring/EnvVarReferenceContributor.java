/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.code.spring;

import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.keyvault.connection.KeyVaultResourceDefinition;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.azure.toolkit.intellij.connector.code.spring.PropertiesCompletionContributor.APPLICATION_PROPERTIES_FILE;
import static com.microsoft.azure.toolkit.intellij.connector.code.spring.YamlCompletionContributor.APPLICATION_YAML_FILE;
import static com.microsoft.azure.toolkit.intellij.keyvault.code.spring.EnvVarCompletionContributor.VALUE_ANNOTATION;

public class EnvVarReferenceContributor extends PsiReferenceContributor {
    static final Pattern pattern = Pattern.compile("\\$\\{[A-Za-z0-9-]{1,127}}");

    @Override
    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PropertyValueImpl.class).inFile(APPLICATION_PROPERTIES_FILE),
                PlatformPatterns.psiElement(YAMLPlainTextImpl.class).withParent(PlatformPatterns.psiElement(YAMLKeyValue.class)).inFile(APPLICATION_YAML_FILE),
                PsiJavaPatterns.psiElement(PsiLiteralExpression.class).insideAnnotationParam(VALUE_ANNOTATION)
            ), // NOTE: not sure why KEYVAULT_SECRET_ENV_VAR_PLACES (uses token types) doesn't work
            new PsiReferenceProvider() {
                @Override
                public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                    return getEnvVarReferences(element).toArray(new PsiReference[0]);
                }
            },
            PsiReferenceRegistrar.HIGHER_PRIORITY
        );
    }

    @Nonnull
    static List<EnvVarReference> getEnvVarReferences(final @Nonnull PsiElement element) {
        final String text = element.getText();
        final boolean hasVaults = Optional.ofNullable(ModuleUtil.findModuleForPsiElement(element)).map(AzureModule::from)
            .map(m -> m.hasValidConnections(KeyVaultResourceDefinition.INSTANCE)).orElse(false);
        if (EnvVarCompletionContributor.hasEnvVars(text) && hasVaults) {
            return getEnvVarRanges(element.getText()).stream()
                .map(r -> new EnvVarReference(element, r))
                .toList();
        }
        return Collections.emptyList();
    }

    @Nonnull
    private static List<TextRange> getEnvVarRanges(String text) {
        int startPosition = 0;
        final List<TextRange> ranges = new ArrayList<>();
        final Matcher matcher = EnvVarReferenceContributor.pattern.matcher(text);
        while (startPosition < text.length() - 3 && matcher.find(startPosition)) {
            ranges.add(new TextRange(matcher.start() + 2, matcher.end() - 1));
            startPosition = matcher.end();
        }
        return ranges;
    }
}