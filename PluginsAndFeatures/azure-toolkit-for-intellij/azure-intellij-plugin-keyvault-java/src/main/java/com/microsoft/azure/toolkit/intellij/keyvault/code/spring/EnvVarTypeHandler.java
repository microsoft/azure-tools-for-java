/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.code.spring;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.lang.ASTNode;
import com.intellij.lang.properties.parsing.PropertiesTokenTypes;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.psi.impl.YAMLFileImpl;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.keyvault.code.spring.EnvVarCompletionContributor.ANNOTATION_VALUE;

public class EnvVarTypeHandler extends TypedHandlerDelegate {
    @Override
    public @Nonnull Result checkAutoPopup(char charTyped, @Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        if (DumbService.isDumb(project) // indexing
            || !EnvVarCompletionContributor.SPECIAL_CHARS.contains(charTyped) // not related special chars
            || !(file instanceof PropertiesFileImpl || file instanceof YAMLFileImpl || file instanceof PsiJavaFile)) {
            return Result.CONTINUE;
        }
        final PsiElement ele = file.findElementAt(editor.getCaretModel().getOffset() - 1); // generated psi element doesn't contain the typed char.
        final boolean isYamlValue = file instanceof YAMLFileImpl &&
            Optional.ofNullable(ele).map(PsiElement::getPrevSibling).map(PsiElement::getLastChild) // case "key: $"
                .or(() -> Optional.ofNullable(ele).map(PsiElement::getParent).filter(e -> e instanceof YAMLPlainTextImpl).map(PsiElement::getPrevSibling).map(PsiElement::getPrevSibling)) // case "key: abc$"
                .filter(e -> e instanceof LeafPsiElement).map(PsiElement::getText).filter(t -> StringUtils.equals(t, ":"))
                .isPresent();
        final boolean isPropertyValue = file instanceof PropertiesFileImpl &&
            Optional.ofNullable(ele).map(PsiElement::getNode).map(ASTNode::getElementType)
                .filter(t -> t == PropertiesTokenTypes.KEY_VALUE_SEPARATOR || t == PropertiesTokenTypes.VALUE_CHARACTERS)
                .isPresent();
        final boolean inValueAnnotation = file instanceof PsiJavaFile && Objects.nonNull(ele) && ANNOTATION_VALUE.accepts(ele) || ANNOTATION_VALUE.accepts(ele.getParent());
        if (isPropertyValue || isYamlValue || inValueAnnotation) {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
            return Result.STOP;
        }
        return Result.CONTINUE;
    }
}
