/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.function;

import com.intellij.codeInsight.completion.CompletionContributor;

public class FunctionAnnotationCompletionContributor extends CompletionContributor {

    public FunctionAnnotationCompletionContributor() {
        super();
        extend(null, FunctionBlobPathCompletionProvider.BLOB_PATH_PATTERN, new FunctionBlobPathCompletionProvider());
        extend(null, FunctionQueueNameCompletionProvider.QUEUE_NAME_PATTERN, new FunctionQueueNameCompletionProvider());
        extend(null, FunctionTableNameCompletionProvider.TABLE_NAME_PATTERN, new FunctionTableNameCompletionProvider());
    }
}
