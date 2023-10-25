/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.completion.resource;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionBlobPathCompletionProvider;
import com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionConnectionCompletionProvider;
import com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionQueueNameCompletionProvider;
import com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionTableNameCompletionProvider;

public class AzureStorageFunctionAnnotationCompletionContributor extends CompletionContributor {

    public AzureStorageFunctionAnnotationCompletionContributor() {
        super();
        // connection
        extend(CompletionType.BASIC, FunctionConnectionCompletionProvider.STORAGE_ANNOTATION_CONNECTION_PATTERN, new FunctionConnectionCompletionProvider());
        // path
        extend(CompletionType.BASIC, FunctionBlobPathCompletionProvider.BLOB_PATH_PATTERN, new FunctionBlobPathCompletionProvider());
        extend(CompletionType.BASIC, FunctionQueueNameCompletionProvider.QUEUE_NAME_PATTERN, new FunctionQueueNameCompletionProvider());
        extend(CompletionType.BASIC, FunctionTableNameCompletionProvider.TABLE_NAME_PATTERN, new FunctionTableNameCompletionProvider());
        // enum
    }
}
