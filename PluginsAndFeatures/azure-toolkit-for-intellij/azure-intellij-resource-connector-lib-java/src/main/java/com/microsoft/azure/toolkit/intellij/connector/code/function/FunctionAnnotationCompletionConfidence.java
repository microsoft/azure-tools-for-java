/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.function;

import com.intellij.codeInsight.completion.CompletionConfidence;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.util.ThreeState;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

import static com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionConnectionCompletionContributor.FUNCTION_ANNOTATION_CONNECTION_PATTERN;

public class FunctionAnnotationCompletionConfidence extends CompletionConfidence {
    public static Set<ElementPattern> FUNCTION_CODE_COMPLETION_PATTERN_SET = new HashSet<>();

    static {
        FUNCTION_CODE_COMPLETION_PATTERN_SET.add(FUNCTION_ANNOTATION_CONNECTION_PATTERN);
    }

    public static void registerCodeCompletionPattern(@Nonnull ElementPattern pattern) {
        FUNCTION_CODE_COMPLETION_PATTERN_SET.add(pattern);
    }

    @Nonnull
    @Override
    public ThreeState shouldSkipAutopopup(@Nonnull PsiElement element, @Nonnull PsiFile psiFile, int offset) {
        if (element.getParent() instanceof PsiLiteralExpression && shouldNotSkip(element)) {
            return ThreeState.NO;
        }
        return ThreeState.UNSURE;
    }

    private static boolean shouldNotSkip(@Nonnull PsiElement element) {
        return FUNCTION_CODE_COMPLETION_PATTERN_SET.stream().anyMatch(pattern -> pattern.accepts(element));
    }
}
