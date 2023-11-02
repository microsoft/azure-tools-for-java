/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.function;

import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionConnectionCompletionContributor.CONNECTION_NAME_VALUE;

public class FunctionConnectionReferenceContributor extends PsiReferenceContributor {
    public static final ElementPattern ACCOUNT_ANNOTATION_PATTERN = psiElement(PsiLiteralExpression.class)
            .insideAnnotationParam("com.microsoft.azure.functions.annotation.StorageAccount");
    public static final ElementPattern CONNECTION_REFERENCE_PATTERN = PlatformPatterns.or(
            ACCOUNT_ANNOTATION_PATTERN,
            psiElement(PsiLiteralExpression.class).withParent(CONNECTION_NAME_VALUE)
    );

    @Override
    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(CONNECTION_REFERENCE_PATTERN, new PsiReferenceProvider() {
            @Override
            public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                final PsiLiteralExpression literal = (PsiLiteralExpression) element;
                final String value = literal.getValue() instanceof String ? (String) literal.getValue() : null;
                if (StringUtils.isNotBlank(value)) {
                    final TextRange range = new TextRange(1, value.length() + 1);
                    return new PsiReference[]{new FunctionAnnotationResourceReference(element, range)};
                }
                return PsiReference.EMPTY_ARRAY;
            }
        });
    }
}
