/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions.helpers.csharp

import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.siblings
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.CSharpLanguage
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.lexer.CSharpTokenType
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.psi.CSharpStringLiteralExpression

object AzureFunctionsPsiHelper {

    fun tryResolveTimerTriggerAttributeFromScheduleExpressionStringLiteralSibling(element: PsiElement?): PsiElement? {
        if (element == null) return null
        if (element.language != CSharpLanguage) return null

        if (element.elementType != CSharpTokenType.STRING_LITERAL_REGULAR) return null

        // [TimerTrigger("<-- search backward from here
        val timerTriggerAttributeSibling = element.parent.siblings(forward = false)
                .takeWhile { it.text != "[" }
                .firstOrNull { it.text == "TimerTrigger" || it.text == "TimerTriggerAttribute" }

        return timerTriggerAttributeSibling
    }

    fun tryResolveTimerTriggerScheduleExpressionStringLiteral(element: PsiElement?): PsiElement? {
        if (element == null) return null
        if (element.language != CSharpLanguage) return null

        if (element.text != "[") return null

        val timerTriggerAttributeSibling = element.siblings(forward = true)
                .takeWhile { it.text != "]" }
                .firstOrNull { it.text == "TimerTrigger" || it.text == "TimerTriggerAttribute" }
                ?: return null

        // [TimerTrigger("* * * * * *")] - take the string content
        val timerTriggerScheduleExpressionStringLiteralSibling = timerTriggerAttributeSibling.siblings(forward = true)
                .takeWhile { it.text != "]" && it.text != ")" }
                .firstOrNull { it is CSharpStringLiteralExpression }
                ?.children?.firstOrNull { it.elementType == CSharpTokenType.STRING_LITERAL_REGULAR }

        return timerTriggerScheduleExpressionStringLiteralSibling;
    }
}