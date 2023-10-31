/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code;

import com.intellij.codeInsight.intention.choice.ChoiceVariantIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPsiElementPointer;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

@AllArgsConstructor
public class ChangeEnvironmentVariableFix extends ChoiceVariantIntentionAction {
    private String origin;
    private String value;
    private SmartPsiElementPointer<PsiElement> pointer;

    @Override
    public int getIndex() {
        return 0;
    }

    @Override
    public @IntentionName
    @Nonnull String getName() {
        return String.format("Change to \"%s\"", value);
    }

    @Override
    public @IntentionFamilyName
    @Nonnull String getFamilyName() {
        return "Resource Connection Fixes";
    }

    @Override
    public void applyFix(@Nonnull Project project, PsiFile file, @Nullable Editor editor) {
        if (Objects.isNull(editor)) {
            return;
        }
        final Document document = editor.getDocument();
        final PsiElement element = pointer.getElement();
        final TextRange textRange = element.getTextRange();
        final String newValue = StringUtils.isEmpty(origin) ? String.format("\"%s\"", value) : StringUtils.replace(element.getText(), origin, value);
        document.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), newValue);
        PsiDocumentManager.getInstance(project).commitDocument(document);
    }
}
