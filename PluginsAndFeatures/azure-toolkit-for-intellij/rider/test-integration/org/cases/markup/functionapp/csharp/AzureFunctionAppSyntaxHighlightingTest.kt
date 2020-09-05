/**
 * Copyright (c) 2020 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.cases.markup.functionapp.csharp

import com.intellij.lang.annotation.HighlightSeverity
import com.jetbrains.rdclient.testFramework.waitForDaemon
import com.jetbrains.rider.test.annotations.TestEnvironment
import com.jetbrains.rider.test.base.BaseTestWithMarkup
import com.jetbrains.rider.test.enums.CoreVersion
import org.testng.annotations.Test

@TestEnvironment(coreVersion = CoreVersion.DEFAULT)
class AzureFunctionAppSyntaxHighlightingTest : BaseTestWithMarkup() {

    override fun getSolutionDirectoryName(): String = "FunctionApp"

    @Test
    fun testTimerTrigger_CronExpression_ValidExpression() = verifySyntaxHighlighting()

    @Test
    fun testTimerTrigger_CronExpression_IncompleteExpression() = verifySyntaxHighlighting()

    @Test
    fun testTimerTrigger_CronExpression_EmptyString() = verifySyntaxHighlighting()

    @Test
    fun testTimerTrigger_CronExpression_SpaceString() = verifySyntaxHighlighting()

    @Test
    fun testTimerTrigger_CronExpression_ValidSettingsVariable() = verifySyntaxHighlighting()

    @Test
    fun testTimerTrigger_CronExpression_InvalidSettingsVariable() = verifySyntaxHighlighting()

    private fun verifySyntaxHighlighting() =
            doTestWithMarkupModel(
                    testFilePath = "FunctionApp/Function.cs",
                    sourceFileName = "Function.cs",
                    goldFileName = "Function.gold"
            ) {
                waitForDaemon()
                dumpHighlightersTree(HighlightSeverity.ERROR)
            }
}
