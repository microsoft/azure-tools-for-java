/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.function;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.impl.source.PsiJavaFileImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionConnectionCompletionContributor.CONNECTION_NAME_VALUE;
import static com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionConnectionCompletionContributor.STORAGE_ACCOUNT;

public class FunctionAnnotationTypeHandler extends TypedHandlerDelegate {

    public static final Set<ElementPattern> FUNCTION_ANNOTATION_KEY_PAIR_PATTERN_SET = new HashSet<>();
    public static final ElementPattern STORAGE_ACCOUNT_VALUE_PATTERN =
            psiElement().withSuperParent(2, PsiJavaPatterns.psiAnnotation().qName(STORAGE_ACCOUNT));

    static {
        FUNCTION_ANNOTATION_KEY_PAIR_PATTERN_SET.add(CONNECTION_NAME_VALUE);
    }

    public static void registerKeyPairPattern(@Nonnull ElementPattern pattern) {
        FUNCTION_ANNOTATION_KEY_PAIR_PATTERN_SET.add(pattern);
    }

    @Override
    public @Nonnull Result checkAutoPopup(char charTyped, @Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        if (DumbService.isDumb(project) || !(file instanceof PsiJavaFileImpl)) {
            return Result.CONTINUE;
        }
        final PsiElement ele = file.findElementAt(editor.getCaretModel().getOffset());
        if (Objects.isNull(ele)) {
            return Result.CONTINUE;
        }
        if (charTyped == '"') {
            final PsiNameValuePair nameValuePair = getPrevNameValuePair(ele);
            if (STORAGE_ACCOUNT_VALUE_PATTERN.accepts(ele)
                    || (Objects.nonNull(nameValuePair) && FUNCTION_ANNOTATION_KEY_PAIR_PATTERN_SET.stream().anyMatch(pattern -> pattern.accepts(nameValuePair)))) {
                AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
                return Result.STOP;
            }
        }
        return Result.CONTINUE;
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
