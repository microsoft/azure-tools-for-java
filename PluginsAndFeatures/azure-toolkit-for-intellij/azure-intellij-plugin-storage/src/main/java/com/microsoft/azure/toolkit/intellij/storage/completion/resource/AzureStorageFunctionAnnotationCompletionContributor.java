/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.completion.resource;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameValuePair;
import com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionBlobPathCompletionProvider;
import com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionConnectionCompletionProvider;
import com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionQueueNameCompletionProvider;
import com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionTableNameCompletionProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.stream.Stream;

import static com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionBlobPathCompletionProvider.BLOB_PATH_PAIR_PATTERN;
import static com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionConnectionCompletionProvider.CONNECTION_NAME_VALUE;
import static com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionConnectionCompletionProvider.STORAGE_ANNOTATION_CONNECTION_PATTERN;
import static com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionQueueNameCompletionProvider.QUEUE_NAME_PAIR_PATTERN;
import static com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionTableNameCompletionProvider.TABLE_NAME_PAIR_PATTERN;

public class AzureStorageFunctionAnnotationCompletionContributor extends CompletionContributor {

    public AzureStorageFunctionAnnotationCompletionContributor() {
        super();
        // connection
        extend(null, STORAGE_ANNOTATION_CONNECTION_PATTERN, new FunctionConnectionCompletionProvider());
        // path
        extend(null, FunctionBlobPathCompletionProvider.BLOB_PATH_PATTERN, new FunctionBlobPathCompletionProvider());
        extend(null, FunctionQueueNameCompletionProvider.QUEUE_NAME_PATTERN, new FunctionQueueNameCompletionProvider());
        extend(null, FunctionTableNameCompletionProvider.TABLE_NAME_PATTERN, new FunctionTableNameCompletionProvider());
        // enum
    }

    public static boolean shouldScheduleAutoPopup(char charTyped, PsiElement ele, Editor editor) {
        if (charTyped == '=' || charTyped == '"') {
            final PsiElement prevSibling = getPrevNameValuePair(ele);
            return Stream.of(CONNECTION_NAME_VALUE, BLOB_PATH_PAIR_PATTERN, QUEUE_NAME_PAIR_PATTERN, TABLE_NAME_PAIR_PATTERN)
                    .anyMatch(pattern -> pattern.accepts(prevSibling));
        }
        return false;
    }

    @Nullable
    private static PsiNameValuePair getPrevNameValuePair(@Nonnull final PsiElement ele) {
        PsiElement prevSibling = ele.getPrevSibling();
        while (prevSibling != null && !(prevSibling instanceof PsiNameValuePair)) {
            prevSibling = prevSibling.getPrevSibling();
        }
        return (PsiNameValuePair) prevSibling;
    }
}
