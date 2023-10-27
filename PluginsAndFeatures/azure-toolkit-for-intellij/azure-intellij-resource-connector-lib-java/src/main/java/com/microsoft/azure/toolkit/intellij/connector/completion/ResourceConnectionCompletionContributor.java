/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.microsoft.azure.toolkit.intellij.connector.completion.function.FunctionConnectionCompletionProvider;

import static com.microsoft.azure.toolkit.intellij.connector.completion.function.FunctionConnectionCompletionProvider.FUNCTION_ANNOTATION_CONNECTION_PATTERN;

public class ResourceConnectionCompletionContributor extends CompletionContributor {
    public ResourceConnectionCompletionContributor() {
        super();
        extend(null, FUNCTION_ANNOTATION_CONNECTION_PATTERN, new FunctionConnectionCompletionProvider());
//        extend(CompletionType.BASIC, PsiJavaPatterns.psiElement(JavaTokenType.IDENTIFIER), new MethodCompletionProvider());
//        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new AnnotationCompletionProvider());
    }
}
