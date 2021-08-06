/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.language.arm.codeinsight;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.ACTIVATE_TEMPLATE_DEITING;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.ARM;

public class ARMCompletionContributor extends CompletionContributor {

    private static final PsiElementPattern.Capture<PsiElement> AFTER_COLON_IN_PROPERTY = psiElement()
        .afterLeaf(":").withSuperParent(2, JsonProperty.class)
        .andNot(psiElement().withParent(JsonStringLiteral.class));

    private static final PsiElementPattern.Capture<PsiElement> AFTER_COMMA_OR_BRACKET_IN_ARRAY = psiElement()
        .afterLeaf("[", ",").withSuperParent(2, JsonArray.class)
        .andNot(psiElement().withParent(JsonStringLiteral.class));

    public ARMCompletionContributor() {
        // Since the code completion is in early stage, here disable this feature
//        extend(CompletionType.BASIC, psiElement().inside(JsonProperty.class).withLanguage(JsonLanguage.INSTANCE),
//            ARMCompletionProvider.INSTANCE);
        EventUtil.logEvent(EventType.info, ARM, ACTIVATE_TEMPLATE_DEITING, null);
    }

}
