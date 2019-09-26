package org.jetbrains.plugins.azure.functions.run

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.siblings
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.CSharpLanguage
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.lexer.CSharpTokenType
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.psi.CSharpStringLiteralExpression
import com.microsoft.icons.CommonIcons

class AzureFunctionsRunMarkerContributor: RunLineMarkerContributor() {

    companion object {
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

    override fun getInfo(element: PsiElement): Info? {
        tryResolveAzureFunctionName(element) ?: return null

        return Info(CommonIcons.AzureFunctions.FunctionAppRunConfiguration, ExecutorAction.getActions()) { "Run Azure Function" }
    }
}