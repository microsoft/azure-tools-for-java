/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.code.function;

import com.intellij.codeInsight.completion.CompletionContributor;

import static com.microsoft.azure.toolkit.intellij.cosmos.code.function.CosmosDBContainerNameCompletionProvider.COSMOS_CONTAINER_PATTERN;
import static com.microsoft.azure.toolkit.intellij.cosmos.code.function.CosmosDBDatabaseNameCompletionProvider.COSMOS_DATABASE_PATTERN;

public class AzureCosmosDBFunctionAnnotationCompletionContributor extends CompletionContributor {
    public static final String[] COSMOS_ANNOTATIONS = new String[]{
            "com.microsoft.azure.functions.annotation.CosmosDBInput",
            "com.microsoft.azure.functions.annotation.CosmosDBOutput",
            "com.microsoft.azure.functions.annotation.CosmosDBTrigger",
    };

    public AzureCosmosDBFunctionAnnotationCompletionContributor() {
        super();
        extend(null, COSMOS_DATABASE_PATTERN, new CosmosDBDatabaseNameCompletionProvider());
        extend(null, COSMOS_CONTAINER_PATTERN, new CosmosDBContainerNameCompletionProvider());
    }

}
