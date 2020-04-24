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

package org.jetbrains.plugins.azure.functions.completion.csharp

import com.cronutils.descriptor.CronDescriptor
import com.cronutils.parser.CronParser
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.CSharpLanguage
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.lexer.CSharpTokenType
import org.jetbrains.plugins.azure.functions.helpers.NCrontabCronDefinition
import org.jetbrains.plugins.azure.functions.helpers.csharp.AzureFunctionsPsiHelper
import java.util.*

class AzureFunctionsTimerTriggerCompletionContributor : CompletionContributor() {

    private val logger = Logger.getInstance(AzureFunctionsTimerTriggerCompletionContributor::class.java)

    private val cronParser = CronParser(NCrontabCronDefinition)
    private val cronDescriptor = CronDescriptor.instance(Locale.getDefault())
    private val cronSuggestions = arrayOf(
            "* * * * * *",
            "0 * * * * *",
            "0 */5 * * * *",
            "0 0 * * * *",
            "0 0 */6 * * *",
            "0 0 8-18 * * *",
            "0 0 0 * * *",
            "0 0 10 * * *",
            "0 0 * * * 1-5",
            "0 0 0 * * 0",
            "0 0 9 * * Mon",
            "0 0 0 1 * *",
            "0 0 0 1 1 *",
            "0 0 * * * Sun",
            "0 0 0 * * Sat,Sun",
            "0 0 0 * * 6,0",
            "0 0 0 1-7 * Sun",
            "11 5 23 * * *",
            "*/15 * * * * *",
            "0 30 9 * Jan Mon"
    )

    init {
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(CSharpTokenType.STRING_LITERAL_REGULAR).withLanguage(CSharpLanguage),
                object : CompletionProvider<CompletionParameters?>() {

                    override fun addCompletions(
                            parameters: CompletionParameters,
                            context: ProcessingContext,
                            resultSet: CompletionResultSet) {

                        val element = parameters.position
                        val timerTriggerAttributeElement = AzureFunctionsPsiHelper.tryResolveTimerTriggerAttributeFromScheduleExpressionStringLiteralSibling(element)
                        if (timerTriggerAttributeElement != null) {
                            for (cronSuggestion in cronSuggestions) {

                                try {
                                    val cron = cronParser.parse(cronSuggestion)
                                    val description = cronDescriptor.describe(cron)

                                    resultSet.addElement(LookupElementBuilder.create(cronSuggestion)
                                            .withTypeText(description))
                                } catch (e: IllegalArgumentException) {
                                    logger.error("Error while adding completion suggestion '$cronSuggestion'", e)
                                }
                            }
                        }
                    }
                }
        )
    }
}