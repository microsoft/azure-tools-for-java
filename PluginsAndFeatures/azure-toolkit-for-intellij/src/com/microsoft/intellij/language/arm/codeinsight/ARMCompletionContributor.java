/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
