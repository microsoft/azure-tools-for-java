/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm.language;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class ARMTemplateCompletionContributor extends CompletionContributor {

    private static final PsiElementPattern.Capture<PsiElement> AFTER_COLON_IN_PROPERTY = psiElement()
        .afterLeaf(":").withSuperParent(2, JsonProperty.class)
        .andNot(psiElement().withParent(JsonStringLiteral.class));

    private static final PsiElementPattern.Capture<PsiElement> AFTER_COMMA_OR_BRACKET_IN_ARRAY = psiElement()
        .afterLeaf("[", ",").withSuperParent(2, JsonArray.class)
        .andNot(psiElement().withParent(JsonStringLiteral.class));

    public ARMTemplateCompletionContributor() {
        // Since the code completion is in early stage, here disable this feature
        // extend(CompletionType.BASIC, psiElement().inside(JsonProperty.class).withLanguage(JsonLanguage.INSTANCE), ARMCompletionProvider.INSTANCE);
    }

}
