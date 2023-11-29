/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.code.spring;

import com.intellij.codeInsight.completion.CompletionConfidence;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ThreeState;

import javax.annotation.Nonnull;

/**
 * https://intellij-support.jetbrains.com/hc/en-us/community/posts/206114249-How-to-complete-string-literal-expressions-
 */
public class EnvVarConfidence extends CompletionConfidence {

    @Nonnull
    @Override
    public ThreeState shouldSkipAutopopup(@Nonnull PsiElement element, @Nonnull PsiFile psiFile, int offset) {
        final String text = element.getText().replace("\"", "");
        if (EnvVarCompletionContributor.ANNOTATION_VALUE.accepts(element) && text.contains("$")) {
            return ThreeState.NO;
        }
        return ThreeState.UNSURE;
    }
}