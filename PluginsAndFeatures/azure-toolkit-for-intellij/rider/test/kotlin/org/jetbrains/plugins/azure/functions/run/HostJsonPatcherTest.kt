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

import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rdclient.util.idea.toIOFile
import com.jetbrains.rider.test.asserts.shouldBe
import com.jetbrains.rider.test.asserts.shouldBeFalse
import com.jetbrains.rider.test.asserts.shouldBeTrue
import com.jetbrains.rider.test.asserts.shouldContain
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class HostJsonPatcherTest {

    companion object {
        private val hostJsonContent = StringBuilder()
                .appendln("{")
                .appendln("  \"version\": \"2.0\"")
                .append("}")
                .toString()
    }

    private val tempDir: File by lazy {
        val dir = FileUtil.getTempDirectory().toIOFile()
        val toCreate = dir.resolve(javaClass.simpleName)
        if (!toCreate.exists()) {
            toCreate.mkdirs()
            toCreate.deleteOnExit()
        }
        toCreate
    }

    private val hostJsonFile: File by lazy {
        val file = tempDir.resolve("host.json")
        if (!file.exists()) {
            file.createNewFile()
            file.deleteOnExit()
        }
        file
    }

    @BeforeMethod
    fun setupWorkingDir() {
        hostJsonFile.writeText(hostJsonContent)
    }

    @AfterMethod(alwaysRun = true)
    fun cleanupWorkingDir() {
        if (tempDir.exists())
            tempDir.listFiles().orEmpty().forEach { it.deleteRecursively() }
    }

    @Test
    fun testPatchHostJsonFile_FileDoesNotExist_NotUpdated() {
        FileUtil.rename(hostJsonFile, "host_1.json")
        val renamedHostJsonFile = hostJsonFile.parentFile.resolve("host_1.json")

        val workingDir = renamedHostJsonFile.parent

        checkWarningContains("WARN: Could not find host.json file to patch.") {
            HostJsonPatcher.tryPatchHostJsonFile(workingDirectory = workingDir, functionNames = "function")
        }

        hostJsonFile.exists().shouldBeFalse()
        renamedHostJsonFile.readText().shouldBe(hostJsonContent)
    }

    @Test
    fun testPatchHostJsonFile_RootDir_Updated() {
        val workingDir = hostJsonFile.parent
        HostJsonPatcher.tryPatchHostJsonFile(workingDirectory = workingDir, functionNames = "function")

        hostJsonFile.exists().shouldBeTrue()
        hostJsonFile.readText().shouldBe(StringBuilder()
                .appendln("{")
                .appendln("  \"version\": \"2.0\",")
                .appendln("  \"functions\": [")
                .appendln("    \"function\"")
                .appendln("  ]")
                .append("}").toString())
    }

    @Test
    fun testPatchHostJsonFile_ParentDir_Updated() {
        val workingDir = hostJsonFile.parentFile.resolve("parent")
        workingDir.mkdirs()

        HostJsonPatcher.tryPatchHostJsonFile(workingDirectory = workingDir.path, functionNames = "function")
        hostJsonFile.readText().shouldBe(StringBuilder()
                .appendln("{")
                .appendln("  \"version\": \"2.0\",")
                .appendln("  \"functions\": [")
                .appendln("    \"function\"")
                .appendln("  ]")
                .append("}").toString())
    }

    @Test
    fun testPatchHostJsonFile_ParentOfParentDir_Updated() {
        val workingDir = hostJsonFile.parentFile.resolve("parent").resolve("parent")
        workingDir.mkdirs()

        HostJsonPatcher.tryPatchHostJsonFile(workingDirectory = workingDir.path, functionNames = "function")
        hostJsonFile.readText().shouldBe(StringBuilder()
                .appendln("{")
                .appendln("  \"version\": \"2.0\",")
                .appendln("  \"functions\": [")
                .appendln("    \"function\"")
                .appendln("  ]")
                .append("}").toString())
    }

    @Test
    fun testPatchHostJsonFile_ParentOfParentOfParentDir_NotUpdated() {
        val workingDir = hostJsonFile.parentFile.resolve("parent").resolve("parent").resolve("parent")
        workingDir.mkdirs()

        checkWarningContains("WARN: Could not find host.json file to patch.") {
            HostJsonPatcher.tryPatchHostJsonFile(workingDirectory = workingDir.path, functionNames = "function")
        }

        hostJsonFile.readText().shouldBe(hostJsonContent)
    }

    @Test
    fun testPatchHostJsonFile_MultipleFunctions_Updated() {
        val workingDir = hostJsonFile.parent
        HostJsonPatcher.tryPatchHostJsonFile(workingDirectory = workingDir, functionNames = "function1, function2")

        hostJsonFile.readText().shouldBe(StringBuilder()
                .appendln("{")
                .appendln("  \"version\": \"2.0\",")
                .appendln("  \"functions\": [")
                .appendln("    \"function1\",")
                .appendln("    \"function2\"")
                .appendln("  ]")
                .append("}").toString())
    }

    @Test
    fun testPatchHostJsonFile_MultipleHostFiles_Updated() {
        val workingDir = hostJsonFile.parentFile.resolve("parent")
        workingDir.mkdirs()

        val hostJsonFileRoot = workingDir.resolve("host.json")
        hostJsonFileRoot.createNewFile()
        hostJsonFileRoot.writeText(hostJsonContent)

        HostJsonPatcher.tryPatchHostJsonFile(workingDirectory = workingDir.path, functionNames = "function")

        hostJsonFile.exists().shouldBeTrue()
        hostJsonFileRoot.exists().shouldBeTrue()

        hostJsonFile.readText().shouldBe(hostJsonContent)
        hostJsonFileRoot.readText().shouldBe(StringBuilder()
                .appendln("{")
                .appendln("  \"version\": \"2.0\",")
                .appendln("  \"functions\": [")
                .appendln("    \"function\"")
                .appendln("  ]")
                .append("}").toString())
    }

    @Suppress("SameParameterValue")
    private fun checkWarningContains(warnMessage: String, action: () -> Unit) {
        val originErr = System.err
        try {
            val errorStream = ByteArrayOutputStream()
            System.setErr(PrintStream(errorStream))
            action()
            errorStream.toString().shouldContain(warnMessage)
        } finally {
            System.setErr(originErr)
        }
    }
}
