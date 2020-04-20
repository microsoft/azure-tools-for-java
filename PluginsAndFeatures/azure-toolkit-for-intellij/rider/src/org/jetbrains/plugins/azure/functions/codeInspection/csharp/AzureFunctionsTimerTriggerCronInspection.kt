/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions.codeInspection.csharp

import com.cronutils.descriptor.CronDescriptor
import com.cronutils.parser.CronParser
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.CSharpLanguage
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import org.jetbrains.plugins.azure.functions.helpers.NCrontabCronDefinition
import org.jetbrains.plugins.azure.functions.helpers.csharp.AzureFunctionsPsiHelper
import java.util.*

class AzureFunctionsTimerTriggerCronInspection : LocalInspectionTool() {

    private val cronParser = CronParser(NCrontabCronDefinition)
    private val cronDescriptor = CronDescriptor.instance(Locale.getDefault())

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {

            override fun visitElement(element: PsiElement) {

                if (element.language != CSharpLanguage) return

                val scheduleExpressionStringLiteralSibling = AzureFunctionsPsiHelper.tryResolveTimerTriggerScheduleExpressionStringLiteral(element) ?: return
                val scheduleExpression = scheduleExpressionStringLiteralSibling.text?.replace("\"", "") ?: return

                // Validate expression
                try {
                    val cron = cronParser.parse(scheduleExpression)
                    val description = cronDescriptor.describe(cron)

                    // Valid (because no exception above)
                    val validDescriptor = holder.manager.createProblemDescriptor(
                            scheduleExpressionStringLiteralSibling,
                            StringUtil.capitalize(description),
                            holder.isOnTheFly,
                            null,
                            ProblemHighlightType.WARNING).apply {
                        // Best-looking color style
                        setTextAttributes(DefaultLanguageHighlighterColors.NUMBER)
                    }
                    holder.registerProblem(validDescriptor)
                } catch (e: IllegalArgumentException) {
                    val problemMessage = message("inspection.function_app.timer_trigger.invalid_schedule_expression")
                    val problemDescription =
                            if (!e.message.isNullOrBlank()) { "$problemMessage: ${e.message}" }
                            else { problemMessage }

                    // Invalid
                    holder.registerProblem(scheduleExpressionStringLiteralSibling, problemDescription, ProblemHighlightType.ERROR)
                }
            }
        }
    }
}
