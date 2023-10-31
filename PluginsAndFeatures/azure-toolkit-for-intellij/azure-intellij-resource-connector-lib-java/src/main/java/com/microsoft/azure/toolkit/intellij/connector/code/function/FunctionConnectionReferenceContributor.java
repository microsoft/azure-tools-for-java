/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.function;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionConnectionCompletionProvider.CONNECTION_NAME_VALUE;

public class FunctionConnectionReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar) {
        // blob
        registrar.registerReferenceProvider(psiElement(PsiLiteralExpression.class).inside(CONNECTION_NAME_VALUE), new PsiReferenceProvider() {
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
