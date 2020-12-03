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

package org.jetbrains.plugins.azure.functions.run

import com.jetbrains.rider.model.ProjectOutput
import com.jetbrains.rider.test.asserts.shouldBe
import org.testng.annotations.Test
import java.io.File

class AzureFunctionsRunnableProjectUtilTest {

    @Test
    fun testPatchProjectOutput_DefaultArguments_Defaults() {
        val projectOutput = ProjectOutput(
                tfm = ".NetFramework 4.7.1",
                exePath = "/path/to/executable",
                defaultArguments = emptyList(),
                workingDirectory = "/path/to/working/dir",
                dotNetCorePlatformRoot = "/root")

        val patched = AzureFunctionsRunnableProjectUtil.patchProjectOutput(projectOutput)
        patched.tfm.shouldBe(".NetFramework 4.7.1")
        patched.exePath.shouldBe("/path/to/executable")
        patched.defaultArguments.shouldBe(listOf("host", "start", "--pause-on-error"))
        patched.workingDirectory.shouldBe("/path/to/working/dir")
        patched.dotNetCorePlatformRoot.shouldBe("/root")
    }

    @Test
    fun testPatchProjectOutput_DefaultArguments_SkipExistingArguments() {
        val projectOutput = ProjectOutput(
                tfm = "",
                exePath = "",
                defaultArguments = listOf("customArgument"),
                workingDirectory = "",
                dotNetCorePlatformRoot = "")

        val patched = AzureFunctionsRunnableProjectUtil.patchProjectOutput(projectOutput)
        patched.defaultArguments.shouldBe(listOf("host", "start", "--pause-on-error"))
    }

    @Test
    fun testPatchProjectOutput_WorkingDirectory_EndsWithBin() {
        val workingDir = File("this").resolve("is").resolve("working").resolve("dir").resolve("bin")
        val projectOutput = ProjectOutput(
                tfm = "",
                exePath = "",
                defaultArguments = listOf(),
                workingDirectory = workingDir.path,
                dotNetCorePlatformRoot = "")

        val patched = AzureFunctionsRunnableProjectUtil.patchProjectOutput(projectOutput)
        patched.workingDirectory.shouldBe(workingDir.parent + File.separator)
    }
}
