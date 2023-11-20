/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.function;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.intellij.patterns.PsiJavaPatterns.literalExpression;
import static com.intellij.patterns.PsiJavaPatterns.psiElement;

@RequiredArgsConstructor
public class FunctionAnnotationValueInsertHandler implements InsertHandler<LookupElement> {
    private final boolean popup;
    private final Map<String, String> properties = new HashMap<>();

    public FunctionAnnotationValueInsertHandler(boolean popup, @Nullable final Map<String, String> additionalProperties) {
        this.popup = popup;
        Optional.ofNullable(additionalProperties).ifPresent(properties::putAll);
    }

    @Override
    @AzureOperation(name = "user/connector.insert_properties_from_function_code_completion")
    public void handleInsert(@Nonnull InsertionContext context, @Nonnull LookupElement item) {
        if (popup) {
            AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
        }
        final PsiElement element = PsiUtil.getElementAtOffset(context.getFile(), context.getStartOffset());
        final boolean hasSpace = element.getPrevSibling() instanceof PsiWhiteSpace;
        // handle when insert not happen in string literal
        final String property = element.getText();
        if (!psiElement().inside(literalExpression()).accepts(element)) {
            final String newElementValue = (hasSpace ? StringUtils.EMPTY : StringUtils.SPACE) + String.format("\"%s\"", property);
            context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), newElementValue);
            context.commitDocument();
        }
        final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        if (Objects.isNull(annotation) || MapUtils.isEmpty(properties)) {
            return;
        }
        properties.forEach((key, value) -> updatePropertiesIfEmpty(annotation, key, value, context));
    }

    private void updatePropertiesIfEmpty(@Nonnull final PsiAnnotation annotation, final String key, final String value, @Nonnull InsertionContext context) {
        final PsiAnnotationMemberValue psiValue = Optional.ofNullable(annotation)
                .map(a -> annotation.findDeclaredAttributeValue(key)).orElse(null);
        final String originalValue = Optional.ofNullable(psiValue).map(PsiAnnotationMemberValue::getText)
                .map(s -> s.replace("\"", "")).orElse(null);
        if (Objects.isNull(psiValue) || StringUtils.isNotBlank(originalValue)) {
            return;
        }
        final String newConnectionValue = "\"" + value + "\"";
        context.getDocument().replaceString(psiValue.getTextRange().getStartOffset(), psiValue.getTextRange().getEndOffset(), newConnectionValue);
        context.commitDocument();
    }
}
