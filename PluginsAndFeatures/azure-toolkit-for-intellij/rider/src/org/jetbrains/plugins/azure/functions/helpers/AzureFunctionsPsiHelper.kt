/**
 * Copyright (c) 2019 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions.helpers

import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.siblings
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.CSharpLanguage
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.lexer.CSharpTokenType
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.psi.CSharpStringLiteralExpression

object AzureFunctionsPsiHelper {

    fun tryResolveAzureFunctionName(element: PsiElement?): String? {
        if (element == null) return null
        if (element.language != CSharpLanguage) return null

        if (element.text != "[") return null

        val functionNameAttributeSibling = element.siblings(forward = true)
                .takeWhile { it.text != "]" }
                .firstOrNull { it.text == "FunctionName" || it.text == "FunctionNameAttribute" }
                ?: return null

        val functionNameAttributeClosingRBracketSibling = functionNameAttributeSibling.siblings(forward = true)
                .firstOrNull { it.text == "]" }
                ?: return null

        // Case 0: Do not be tricked by having a FunctionNameAttribute applied on an identifier,
        //         e.g. `[FunctionName("Foo")]string bar` should not match.
        if (functionNameAttributeClosingRBracketSibling.nextSibling.elementType == CSharpTokenType.IDENTIFIER)
            return null

        // Case 1: [FunctionName("Foo")] - take the string content
        val functionNameStringLiteralSibling = functionNameAttributeSibling.siblings(forward = true)
                .takeWhile { it.text != "]" && it.text != ")" }
                .firstOrNull { it is CSharpStringLiteralExpression }
                ?.children?.firstOrNull { it.elementType == CSharpTokenType.STRING_LITERAL_REGULAR }

        if (functionNameStringLiteralSibling != null) {
            return functionNameStringLiteralSibling.text?.replace("\"", "")
        }

        // Case 2: [FunctionName(nameof(Foo.Bar))] - take the last identifier
        val nameofLiteralSibling = functionNameAttributeSibling.siblings(forward = true)
                .takeWhile { it.text != "]" && it.text != ")" }
                .firstOrNull { it.text == "nameof" }

        if (nameofLiteralSibling != null) {
            val nameofFunctionNameLiteralSibling = nameofLiteralSibling.siblings(forward = true)
                    .takeWhile { it.text != ")" }
                    .lastOrNull()

            if (nameofFunctionNameLiteralSibling != null) {
                return nameofFunctionNameLiteralSibling.text?.replace("\"", "")
            }
        }

        return null
    }
}