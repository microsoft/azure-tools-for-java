/**
 * Copyright (c) 2020-2021 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions.run

import com.jetbrains.rider.model.ProjectOutput
import com.jetbrains.rider.model.RdTargetFrameworkId
import com.jetbrains.rider.test.asserts.shouldBe
import org.testng.annotations.Test
import java.io.File

class AzureFunctionsRunnableProjectUtilTest {

    @Test
    fun testPatchProjectOutput_DefaultArguments_Defaults() {
        val projectOutput = ProjectOutput(
                tfm = RdTargetFrameworkId(
                        shortName = "net471",
                        presentableName = ".NetFramework 4.7.1",
                        isNetCoreApp = false,
                        isNetFramework = true),
                exePath = "/path/to/executable",
                defaultArguments = emptyList(),
                workingDirectory = "/path/to/working/dir",
                dotNetCorePlatformRoot = "/root",
                configuration = null)

        val patched = AzureFunctionsRunnableProjectUtil.patchProjectOutput(projectOutput)
        patched.tfm?.presentableName.shouldBe(".NetFramework 4.7.1")
        patched.exePath.shouldBe("/path/to/executable")
        patched.defaultArguments.shouldBe(listOf("host", "start", "--pause-on-error"))
        patched.workingDirectory.shouldBe("/path/to/working/dir")
        patched.dotNetCorePlatformRoot.shouldBe("/root")
    }

    @Test
    fun testPatchProjectOutput_DefaultArguments_SkipExistingArguments() {
        val projectOutput = ProjectOutput(
                tfm = RdTargetFrameworkId(
                        shortName = "",
                        presentableName = "",
                        isNetCoreApp = false,
                        isNetFramework = false),
                exePath = "",
                defaultArguments = listOf("customArgument"),
                workingDirectory = "",
                dotNetCorePlatformRoot = "",
                configuration = null)

        val patched = AzureFunctionsRunnableProjectUtil.patchProjectOutput(projectOutput)
        patched.defaultArguments.shouldBe(listOf("host", "start", "--pause-on-error"))
    }

    @Test
    fun testPatchProjectOutput_WorkingDirectory_EndsWithBin() {
        val workingDir = File("this").resolve("is").resolve("working").resolve("dir").resolve("bin")
        val projectOutput = ProjectOutput(
                tfm = RdTargetFrameworkId(
                        shortName = "",
                        presentableName = "",
                        isNetCoreApp = false,
                        isNetFramework = false),
                exePath = "",
                defaultArguments = listOf(),
                workingDirectory = workingDir.path,
                dotNetCorePlatformRoot = "",
                configuration = null)

        val patched = AzureFunctionsRunnableProjectUtil.patchProjectOutput(projectOutput)
        patched.workingDirectory.shouldBe(workingDir.parent + File.separator)
    }
}
