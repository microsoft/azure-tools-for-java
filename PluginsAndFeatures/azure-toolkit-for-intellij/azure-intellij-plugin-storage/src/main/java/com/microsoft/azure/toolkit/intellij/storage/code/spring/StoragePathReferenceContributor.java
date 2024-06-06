/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.spring;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.lib.storage.IStorageAccount;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;

public class StoragePathReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(psiElement(PsiLiteralExpression.class), new PsiReferenceProvider() {
            @Override
            public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                final PsiLiteralExpression literal = (PsiLiteralExpression) element;
                final String text = element.getText();
                final String valueWithPrefix = literal.getValue() instanceof String ? (String) literal.getValue() : element.getText();
                if ((valueWithPrefix.startsWith("azure-blob://") || valueWithPrefix.startsWith("azure-file://"))) {
                    final String prefix = valueWithPrefix.startsWith("azure-blob://") ? "azure-blob://" : "azure-file://";
                    return getStorageFileReferences(prefix, prefix, literal, null);
                }
                return PsiReference.EMPTY_ARRAY;
            }
        }, PsiReferenceRegistrar.HIGHER_PRIORITY);
    }

    public static PsiReference[] getStorageFileReferences(@Nonnull final String prefix, @Nonnull final String protocol,
                                                          @Nonnull final PsiLiteralExpression element, @Nullable final IStorageAccount account) {
        final String text = element.getText();
        final String valueWithPrefix = element.getValue() instanceof String ? (String) element.getValue() : StringUtils.EMPTY;
        final String[] parts = new TextRange(prefix.length() + 1, valueWithPrefix.length() + 1).substring(text).split("/", -1);
        final List<StoragePathReference> references = new ArrayList<>();
        final int startOffset = prefix.length() + 1;
        int offset = startOffset;
        for (final String part : parts) {
            if (StringUtils.isBlank(part)) {
                break;
            }
            final int endOffset = offset + part.length();
            final TextRange range = new TextRange(offset, endOffset);
            final String fullNameWithPrefix = protocol + new TextRange(startOffset, endOffset).substring(text);
            references.add(new StoragePathReference(element, range, fullNameWithPrefix, account));
            offset = endOffset + 1;
        }
        return references.toArray(new PsiReference[0]);
    }
}