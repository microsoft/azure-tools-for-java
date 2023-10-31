/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.spring;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.PsiJavaFileImpl;

import javax.annotation.Nonnull;
import java.util.Objects;

public class StringLiteralTypeHandler extends TypedHandlerDelegate {
    @Override
    public @Nonnull Result checkAutoPopup(char charTyped, @Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        if (!StringLiteralCompletionContributor.SPECIAL_CHARS.contains(charTyped) || !(file instanceof PsiJavaFileImpl)) {
            return Result.CONTINUE;
        }
        final PsiElement ele = file.findElementAt(editor.getCaretModel().getOffset());
        if (Objects.isNull(ele)) {
            return Result.CONTINUE;
        }
        final String text = ele.getText().replace("\"", "");
        if (StringLiteralCompletionContributor.PREFIX_PLACES.accepts(ele)
            || text.startsWith("azure-blob") || text.startsWith("azure-file")) {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
            return Result.STOP;
        }
        return Result.CONTINUE;
    }
}
