/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.function;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.storage.IStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.blob.BlobContainer;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import com.microsoft.azure.toolkit.lib.storage.queue.Queue;
import com.microsoft.azure.toolkit.lib.storage.table.Table;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.connector.code.AbstractResourceConnectionAnnotator.isAzureFacetEnabled;

public class FunctionAnnotationResourcePathAnnotator implements Annotator {
    @Override
    public void annotate(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        if (!(isAzureFacetEnabled(element) && Azure.az(AzureAccount.class).isLoggedIn())) {
            return;
        }
        if (FunctionBlobPathCompletionProvider.BLOB_PATH_PATTERN.accepts(element)) {
            validateBlobPath(element, holder);
        } else if (FunctionQueueNameCompletionProvider.QUEUE_NAME_PATTERN.accepts(element)) {
            validateQueueName(element, holder);
        } else if (FunctionTableNameCompletionProvider.TABLE_NAME_PATTERN.accepts(element)) {
            validateTableName(element, holder);
        }
    }

    private void validateTableName(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        final IStorageAccount storageAccount = Optional.ofNullable(annotation).map(Utils::getBindingStorageAccount).orElse(null);
        if (Objects.isNull(storageAccount) || Objects.isNull(annotation.findAttribute("tableName"))) {
            return;
        }
        final PsiLiteralExpression literal = (PsiLiteralExpression) element.getParent();
        final String tableName = literal.getValue() instanceof String ? (String) literal.getValue() : StringUtils.EMPTY;
        final Table table = StringUtils.isBlank(tableName) ? null : storageAccount.getTableModule().get(tableName, storageAccount.getResourceGroupName());
        if (Objects.isNull(table)) {
            final String message = StringUtils.isBlank(tableName) ? "Table name could not be empty" :
                    String.format("Could not find table '%s' in account '%s'", tableName, storageAccount.getName());
            holder.newAnnotation(HighlightSeverity.WARNING, message)
                    .range(element.getTextRange())
                    .highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                    .create();
        }
    }

    private void validateQueueName(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        final IStorageAccount storageAccount = Optional.ofNullable(annotation).map(Utils::getBindingStorageAccount).orElse(null);
        if (Objects.isNull(storageAccount) || Objects.isNull(annotation.findAttribute("queueName"))) {
            return;
        }
        final PsiLiteralExpression literal = (PsiLiteralExpression) element.getParent();
        final String queueName = literal.getValue() instanceof String ? (String) literal.getValue() : StringUtils.EMPTY;
        final Queue queue = StringUtils.isBlank(queueName) ? null : storageAccount.getQueueModule().get(queueName, storageAccount.getResourceGroupName());
        if (Objects.isNull(queue)) {
            final String message = StringUtils.isBlank(queueName) ? "QueueName could not be empty" :
                    String.format("Could not find queue '%s' in account '%s'", queueName, storageAccount.getName());
            holder.newAnnotation(HighlightSeverity.WARNING, message)
                    .range(element.getTextRange())
                    .highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                    .create();
        }
    }

    private void validateBlobPath(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        final IStorageAccount storageAccount = Optional.ofNullable(annotation).map(Utils::getBindingStorageAccount).orElse(null);
        if (Objects.isNull(storageAccount) || Objects.isNull(annotation.findAttribute("path"))) {
            return;
        }
        final PsiLiteralExpression literal = (PsiLiteralExpression) element.getParent();
        final String path = literal.getValue() instanceof String ? (String) literal.getValue() : StringUtils.EMPTY;
        final String pathToValid = path.contains("{") ? StringUtils.substringBeforeLast(StringUtils.substringBefore(path, "{"), "/") : path; // get sub path without parameters
        final StorageFile file = StringUtils.isBlank(path) ? null : getFileByPath(pathToValid, storageAccount);
        if (Objects.isNull(file)) {
            final String message = StringUtils.isBlank(path) ? "Path could not be empty" :
                    String.format("Could not find blob container/file '%s' in account '%s'", pathToValid, storageAccount.getName());
            holder.newAnnotation(HighlightSeverity.WARNING, message)
                    .range(element.getTextRange())
                    .highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                    .create();
        }
    }

    @Nullable
    private StorageFile getFileByPath(@Nonnull final String fullPrefix, @Nonnull final IStorageAccount storageAccount) {
        final String container = fullPrefix.contains("/") ? fullPrefix.substring(0, fullPrefix.indexOf("/")) : fullPrefix;
        final String path = fullPrefix.contains("/") ? fullPrefix.substring(fullPrefix.indexOf("/") + 1) : "";
        final BlobContainer blobContainer = storageAccount.getBlobContainerModule().get(container, storageAccount.getResourceGroupName());
        return StringUtils.isEmpty(path) || Objects.isNull(blobContainer) ? blobContainer : blobContainer.getFile(path);
    }
}
