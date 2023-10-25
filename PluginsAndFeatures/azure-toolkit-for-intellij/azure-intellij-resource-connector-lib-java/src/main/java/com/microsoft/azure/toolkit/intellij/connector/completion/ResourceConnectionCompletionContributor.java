/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.JavaTokenType;
import com.microsoft.azure.toolkit.intellij.connector.completion.provider.AnnotationCompletionProvider;
import com.microsoft.azure.toolkit.intellij.connector.completion.provider.MethodCompletionProvider;

public class ResourceConnectionCompletionContributor extends CompletionContributor {
    public ResourceConnectionCompletionContributor() {
        super();
//        extend(CompletionType.BASIC, PsiJavaPatterns.psiElement(JavaTokenType.IDENTIFIER), new MethodCompletionProvider());
//        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new AnnotationCompletionProvider());
    }
}
