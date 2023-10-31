/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.spring;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.patterns.PsiJavaPatterns.literalExpression;
import static com.intellij.patterns.PsiJavaPatterns.psiElement;

public class StringLiteralResourceReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(psiElement(JavaTokenType.STRING_LITERAL).withParent(literalExpression()), new PsiReferenceProvider() {
            @Override
            public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                final PsiLiteralExpression literal = (PsiLiteralExpression) element.getParent();
                final String text = element.getText();
                final String valueWithPrefix = literal.getValue() instanceof String ? (String) literal.getValue() : element.getText();
                if ((valueWithPrefix.startsWith("azure-blob://") || valueWithPrefix.startsWith("azure-file://"))) {
                    final String prefix = valueWithPrefix.startsWith("azure-blob://") ? "azure-blob://" : "azure-file://";
                    final String[] parts = StringUtils.substringAfter(valueWithPrefix, prefix).split("/", -1);
                    final List<StringLiteralResourceReference> references = new ArrayList<>();
                    int startOffset = prefix.length() + 1;
                    for (final String part : parts) {
                        if (StringUtils.isBlank(part)) {
                            break;
                        }
                        final int endOffset = startOffset + part.length();
                        final TextRange range = new TextRange(startOffset, endOffset);
                        references.add(new StringLiteralResourceReference(element, range, new TextRange(1, endOffset).substring(text)));
                        startOffset = endOffset + 1;
                    }
                    return references.toArray(new PsiReference[0]);
                }
                return PsiReference.EMPTY_ARRAY;
            }
        });
    }
}