/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.function;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionAnnotationResourceReference;
import com.microsoft.azure.toolkit.intellij.storage.code.spring.StoragePathReferenceContributor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.storage.IStorageAccount;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.microsoft.azure.toolkit.intellij.storage.code.function.FunctionBlobPathCompletionProvider.BLOB_PATH_PAIR_PATTERN;
import static com.microsoft.azure.toolkit.intellij.storage.code.function.FunctionQueueNameCompletionProvider.QUEUE_NAME_PAIR_PATTERN;
import static com.microsoft.azure.toolkit.intellij.storage.code.function.FunctionTableNameCompletionProvider.TABLE_NAME_PAIR_PATTERN;

public class FunctionStorageAccountResourceReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar) {
        // blob
        registrar.registerReferenceProvider(psiElement(PsiLiteralExpression.class).withParent(BLOB_PATH_PAIR_PATTERN), new PsiReferenceProvider() {
            @Override
            public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                if (!Azure.az(AzureAccount.class).isLoggedIn()) {
                    return PsiReference.EMPTY_ARRAY;
                }
                final PsiLiteralExpression literal = (PsiLiteralExpression) element;
                final String value = literal.getValue() instanceof String ? (String) literal.getValue() : null;
                // todo: parse the storage account within `StringLiteralResourceReferenceContributor`
                final PsiAnnotation annotation = Objects.requireNonNull(PsiTreeUtil.getParentOfType(element, PsiAnnotation.class));
                final IStorageAccount storageAccount = Utils.getBindingStorageAccount(annotation);
                if (Objects.nonNull(storageAccount)) {
                    return StoragePathReferenceContributor.getStorageFileReferences(StringUtils.EMPTY, "azure-blob://", literal, storageAccount);
                }
                return PsiReference.EMPTY_ARRAY;
            }
        });
        // queue
        registrar.registerReferenceProvider(psiElement(PsiLiteralExpression.class).withParent(QUEUE_NAME_PAIR_PATTERN), new PsiReferenceProvider() {
            @Override
            public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                final PsiLiteralExpression literal = (PsiLiteralExpression) element;
                final String value = literal.getValue() instanceof String ? (String) literal.getValue() : null;
                final BiFunction<PsiAnnotation, Connection<?, ?>, AzResource> function = (annotation, connection) -> {
                    final IStorageAccount storageAccount = Utils.getBindingStorageAccount(annotation);
                    return Optional.ofNullable(storageAccount)
                        .map(account -> account.getQueueModule().get(value, account.getResourceGroupName()))
                        .orElse(null);
                };
                if (StringUtils.isNotBlank(value)) {
                    final TextRange range = new TextRange(1, value.length() + 1);
                    return new PsiReference[]{new FunctionAnnotationResourceReference(element, range, function)};
                }
                return PsiReference.EMPTY_ARRAY;
            }
        });
        // table
        registrar.registerReferenceProvider(psiElement(PsiLiteralExpression.class).withParent(TABLE_NAME_PAIR_PATTERN), new PsiReferenceProvider() {
            @Override
            public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                final PsiLiteralExpression literal = (PsiLiteralExpression) element;
                final String value = literal.getValue() instanceof String ? (String) literal.getValue() : StringUtils.EMPTY;
                final BiFunction<PsiAnnotation, Connection<?, ?>, AzResource> function = (annotation, connection) -> {
                    final IStorageAccount storageAccount = Utils.getBindingStorageAccount(annotation);
                    return Optional.ofNullable(storageAccount)
                        .map(account -> account.getTableModule().get(value, account.getResourceGroupName()))
                        .orElse(null);
                };
                if (StringUtils.isNotBlank(value)) {
                    final TextRange range = new TextRange(1, value.length() + 1);
                    return new PsiReference[]{new FunctionAnnotationResourceReference(element, range, function)};
                }
                return PsiReference.EMPTY_ARRAY;
            }
        });
    }
}
