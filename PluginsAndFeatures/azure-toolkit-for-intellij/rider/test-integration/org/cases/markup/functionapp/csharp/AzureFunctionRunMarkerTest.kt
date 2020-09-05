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

import com.jetbrains.rdclient.daemon.util.attributeId
import com.jetbrains.rdclient.testFramework.waitForDaemon
import com.jetbrains.rider.test.annotations.TestEnvironment
import com.jetbrains.rider.test.base.BaseTestWithMarkup
import com.jetbrains.rider.test.enums.CoreVersion
import org.testng.annotations.Test

@TestEnvironment(coreVersion = CoreVersion.DEFAULT)
class AzureFunctionRunMarkerTest : BaseTestWithMarkup() {

    companion object {
        private const val FUNCTION_APP_RUN_MARKER_ATTRIBUTE_ID = "Azure Function App Run Method Gutter Mark"
    }

    override fun getSolutionDirectoryName(): String = "FunctionApp"

    @Test(description = "Check function app gutter mark is shown on a default Http Trigger Function App from item template.")
    fun testFunctionApp_HttpTriggerFromItemTemplate_Detected() = verifyLambdaGutterMark()

    @Test(description = "Check Http Trigger function app that miss required attribute is not marked with Function App gutter mark.")
    fun testFunctionApp_HttpTriggerMissingAttribute_NotDetected() = verifyLambdaGutterMark()

    @Test(description = "Check any function having required attribute is shown with Function App gutter mark.")
    fun testFunctionApp_MethodWithAttribute_Detected() = verifyLambdaGutterMark()

    @Test(description = "Check Timer Trigger function with required attribute is shown with Function App gutter mark.")
    fun testFunctionApp_TimerTriggerFromItemTemplate_Detected() = verifyLambdaGutterMark()

    @Test(description = "Check Http Trigger function having an attribute, but not a correct one is shown without Function App gutter mark.")
    fun testFunctionApp_HttpTriggerWithIncorrectAttribute_NotDetected() = verifyLambdaGutterMark()

    @Test(description = "Check Http Trigger function having multiple attribute including required [FunctionName] is shown with Function App gutter mark.")
    fun testFunctionApp_HttpTriggerWithMultipleAttributes_Detected() = verifyLambdaGutterMark()

    @Test(description = "Check Http Trigger function having a required attribute, but on namespace level is shown without Function App gutter mark.")
    fun testFunctionApp_HttpTriggerWithNamespaceAttribute_NotDetected() = verifyLambdaGutterMark()

    @Test(description = "Check multipole functions inside one class with [FunctionName] required attribute are shown with gutter mark.")
    fun testFunctionApp_MultipleFunctionApps_Detected() = verifyLambdaGutterMark()

    private fun verifyLambdaGutterMark() {
        doTestWithMarkupModel(
                testFilePath = "FunctionApp/Function.cs",
                sourceFileName = "Function.cs",
                goldFileName = "Function.gold"
        ) {
            waitForDaemon()
            dumpHighlightersTree(
                    valueFilter = { it.attributeId.contains(FUNCTION_APP_RUN_MARKER_ATTRIBUTE_ID) }
            )
        }
    }
}
