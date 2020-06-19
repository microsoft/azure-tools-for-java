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

package org.cases.documentmodel.completion.csharp

import com.jetbrains.rider.test.annotations.TestEnvironment
import com.jetbrains.rider.test.base.CompletionTestBase
import com.jetbrains.rider.test.enums.CoreVersion
import com.jetbrains.rider.test.scriptingApi.*
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

@TestEnvironment(coreVersion = CoreVersion.DEFAULT)
class AzureFunctionsTimerTriggerCompletionTest : CompletionTestBase() {

    override fun getSolutionDirectoryName(): String = "FunctionApp"

    override val restoreNuGetPackages: Boolean? = true

    override val checkTextControls: Boolean = false

    private val defaultCronCompletionList = arrayOf(
            "*/15 * * * * *",
            "* * * * * *",
            "0 */5 * * * *",
            "0 * * * * *",
            "0 0 */6 * * *",
            "0 0 * * * *",
            "0 0 * * * Sun",
            "0 0 * * * 1-5",
            "0 0 0 * * *",
            "0 0 0 * * Sat,Sun",
            "0 0 0 * * 0",
            "0 0 0 * * 6,0",
            "0 0 0 1-7 * Sun",
            "0 0 0 1 * *",
            "0 0 0 1 1 *",
            "0 0 8-18 * * *",
            "0 0 9 * * Mon",
            "0 0 10 * * *",
            "0 30 9 * Jan Mon",
            "11 5 23 * * *"
    )

    @Test(description = "Check the full list of completion variants on opening double quote.")
    fun testPopup_CompletionList_FullListOnQuote() {
        withOpenedEditor("Function.cs", "Function.cs") {
            typeWithLatency("\"")
            assertLookupContainsExact(*defaultCronCompletionList)
        }
    }

    @Test(description = "Check the full list of completion variants when calling a basic completion.")
    fun testPopup_CompletionList_FullListOnBasicCompletion() {
        dumpOpenedEditor("Function.cs", "Function.cs") {
            callBasicCompletion()
            assertLookupContainsExact(*defaultCronCompletionList)
            completeWithEnter()
        }
    }

    @Test(description = "Check completion variants are filtered by a digit prefix.")
    fun testPopup_CompletionList_DigitPrefix() {
        withOpenedEditor("Function.cs", "Function.cs") {
            typeWithLatency("0")
            val expectedCompletionList = defaultCronCompletionList.filter { it.contains("0") }.toTypedArray()
            assertLookupContainsExact(*expectedCompletionList)
        }
    }

    @Test(description = "Check completion variants are filtered by a literal prefix.")
    fun testPopup_CompletionList_LiteralPrefix() {
        withOpenedEditor("Function.cs", "Function.cs") {
            typeWithLatency("Sun")
            val expectedCompletionList = defaultCronCompletionList.filter { it.contains("Sun") }.toTypedArray()
            assertLookupContainsExact(*expectedCompletionList)
        }
    }

    @DataProvider(name = "completionSymbolPrefixData")
    fun completionSymbolPrefixData() = arrayOf(
            arrayOf("Star", "*"),
            arrayOf("Slash", "/"),
            arrayOf("Comma", ","),
            arrayOf("Space", " ")
    )

    // TODO: We might think about re-working this to allow these symbols in completion porefix if it does not break default behavior.
    @Test(description = "Check completion list is not show on termination symbols.",
            dataProvider = "completionSymbolPrefixData")
    fun testPopup_CompletionList_SymbolPrefix(name: String, toType: String) {
        withOpenedEditor("Function.cs", "Function.cs") {
            typeWithLatency(toType)
            ensureThereIsNoLookup()
        }
    }

    @Test(description = "Check completion variants are filtered by full match when type different prefix.")
    fun testPopup_CompletionList_MixedPrefix() {
        withOpenedEditor("Function.cs", "Function.cs") {
            typeWithLatency("0")
            assertLookupContainsExact(*defaultCronCompletionList.filter { it.startsWith("0") }.toTypedArray())

            typeWithLatency(" ")
            assertLookupContainsExact(*defaultCronCompletionList.filter { it.startsWith("0 ") }.toTypedArray())

            typeWithLatency("0")
            assertLookupContainsExact(*defaultCronCompletionList.filter { it.contains("0.+0".toRegex()) }.toTypedArray())

            typeWithLatency(" 0")
            assertLookupContainsExact(*defaultCronCompletionList.filter { it.contains("0.+0.+0".toRegex()) }.toTypedArray())
        }
    }
}
