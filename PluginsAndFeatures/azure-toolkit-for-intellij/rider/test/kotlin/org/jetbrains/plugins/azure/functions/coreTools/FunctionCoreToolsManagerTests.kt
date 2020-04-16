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

package org.jetbrains.plugins.azure.functions.coreTools

import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rider.test.asserts.shouldBe
import com.jetbrains.rider.test.asserts.shouldBeNull
import com.jetbrains.rider.test.asserts.shouldBeTrue
import com.jetbrains.rider.test.asserts.shouldNotBeNull
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File

class FunctionCoreToolsManagerTests {

    var tempDownloadDirectory: File? = null

    @BeforeMethod
    fun setupTestDirectory() {
        tempDownloadDirectory = FileUtil.createTempDirectory("azure-test-core-tools", null)
    }

    @AfterMethod
    fun cleanCoreToolsDirectory() {
        if (tempDownloadDirectory?.exists() == true) {
            tempDownloadDirectory?.deleteRecursively()
        }
    }

    @Test
    fun determineLatestLocalCoreToolsPath_NotExists() {
        val version = FunctionsCoreToolsManager.determineLatestLocalCoreToolsPath()
        version.shouldBeNull("Version for non-existing function core tools directory must return NULL.")
    }

    @Test
    fun determineLatestLocalCoreToolsPath_NotInstalled() {
        val path = tempDownloadDirectory.shouldNotBeNull()
        val version = FunctionsCoreToolsManager.determineLatestLocalCoreToolsPath(toolsDownloadPath = path.path)
        version.shouldBeNull("Latest version for non-installed function core tools must return NULL.")
    }

    @Test
    fun determineLatestLocalCoreToolsPath_SingleVersion() {
        val path = tempDownloadDirectory.shouldNotBeNull()
        prepareCoreToolsDirs(path, arrayOf("3.0.2358"))

        val version = FunctionsCoreToolsManager.determineLatestLocalCoreToolsPath(toolsDownloadPath = path.path)
        val toolsVersion = version.shouldNotBeNull("Latest version for an existing function core tools directory must return a valid version.")
        toolsVersion.shouldBe(path.resolve("3.0.2358").path)
    }

    @Test
    fun determineLatestLocalCoreToolsPath_MultipleVersions() {
        val path = tempDownloadDirectory.shouldNotBeNull()
        prepareCoreToolsDirs(path, arrayOf("3.0.2358", "3.0.2222", "2.0.0001"))

        val version = FunctionsCoreToolsManager.determineLatestLocalCoreToolsPath(toolsDownloadPath = path.path)
        val toolsVersion = version.shouldNotBeNull("Latest version for an existing function core tools directory must return a valid version.")
        toolsVersion.shouldBe(path.resolve("3.0.2358").path)
    }

    private fun prepareCoreToolsDirs(coreToolsDir: File, versions: Array<String>) {
        versions.forEach { version ->
            val versionDir = coreToolsDir.resolve(version)
            versionDir.mkdirs().shouldBeTrue("Failed to create folder with name: ${versionDir.canonicalPath}")
        }
    }
}
