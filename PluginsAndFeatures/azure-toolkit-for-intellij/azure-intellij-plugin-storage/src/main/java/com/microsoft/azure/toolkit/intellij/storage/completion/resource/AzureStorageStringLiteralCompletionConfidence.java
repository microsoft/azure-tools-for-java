/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.completion.resource;

import com.intellij.codeInsight.completion.CompletionConfidence;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.util.ThreeState;

import javax.annotation.Nonnull;

import static com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionBlobPathCompletionProvider.BLOB_PATH_PATTERN;
import static com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionConnectionCompletionProvider.STORAGE_ANNOTATION_CONNECTION_PATTERN;
import static com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionQueueNameCompletionProvider.QUEUE_NAME_PATTERN;
import static com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionTableNameCompletionProvider.TABLE_NAME_PATTERN;

/**
 * https://intellij-support.jetbrains.com/hc/en-us/community/posts/206114249-How-to-complete-string-literal-expressions-
 */
public class AzureStorageStringLiteralCompletionConfidence extends CompletionConfidence {

    @Nonnull
    @Override
    public ThreeState shouldSkipAutopopup(@Nonnull PsiElement element, @Nonnull PsiFile psiFile, int offset) {
        final String text = element.getText().replace("\"", "");
        if (element.getParent() instanceof PsiLiteralExpression && shouldNotSkip(element, text)) {
            return ThreeState.NO;
        }
        return ThreeState.UNSURE;
    }

    private static boolean shouldNotSkip(@Nonnull PsiElement element, String text) {
        return AzureStorageJavaCompletionContributor.PREFIX_PLACES.accepts(element) ||
                STORAGE_ANNOTATION_CONNECTION_PATTERN.accepts(element) ||
                BLOB_PATH_PATTERN.accepts(element) ||
                QUEUE_NAME_PATTERN.accepts(element) ||
                TABLE_NAME_PATTERN.accepts(element) ||
                text.startsWith("azure-blob://") ||
                text.startsWith("azure-file://");
    }
}