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
import com.intellij.util.ResourceUtil
import com.jetbrains.rdclient.util.idea.toIOFile
import com.jetbrains.rider.test.asserts.*
import org.testng.annotations.AfterMethod
import org.testng.annotations.Test
import java.io.File

class FunctionsCoreToolsManagerTests {

    private val tempDir: File by lazy {
        val dir = FileUtil.getTempDirectory().toIOFile()
        val toCreate = dir.resolve(javaClass.simpleName)
        if (!toCreate.exists()) {
            toCreate.mkdirs()
            toCreate.deleteOnExit()
        }
        toCreate
    }

    private val azureTestCoreToolsDir: File by lazy {
        val toCreate = tempDir.resolve("azure-test-core-tools")
        if (!toCreate.exists()) {
            toCreate.mkdirs()
            toCreate.deleteOnExit()
        }
        toCreate
    }

    @AfterMethod(alwaysRun = true)
    fun cleanupWorkingDir() {
        if (tempDir.exists())
            tempDir.listFiles().orEmpty().forEach { it.deleteRecursively() }
    }

    //region determineVersion()

    @Test
    fun testDetermineVersion_NullablePath() {
        val version = FunctionsCoreToolsManager.determineVersion(coreToolsPath = null)
        version.shouldBeNull(
                "Function core tools version for an null function core tools directory must return NULL version.")
    }

    @Test
    fun testDetermineVersion_NonExistingPath() {
        val coreToolPath = File("non").resolve("existing").resolve("path").path
        val version =
                FunctionsCoreToolsManager.determineVersion(coreToolsPath = coreToolPath)
        version.shouldBeNull(
                "Function core tools version for a non-existing function core tools directory must return NULL version.")
    }

    @Test
    fun testDetermineVersion_ValidVersion() {
        val resourceToolPath =
                ResourceUtil.getResource(this::class.java.classLoader, ".", "tools").path.toIOFile()

        resourceToolPath.copyRecursively(azureTestCoreToolsDir, true)

        val versionValue =
                FunctionsCoreToolsManager.determineVersion(azureTestCoreToolsDir.path)

        val version = versionValue.shouldNotBeNull()
        version.version.shouldBe("3.0.2009")
    }

    @Test(description = "Executable flag should be set inside a call.")
    fun testDetermineVersion_NoExecutableFlag() {
        val resourceToolPath =
                ResourceUtil.getResource(this::class.java.classLoader, ".", "tools").path.toIOFile()

        resourceToolPath.copyRecursively(azureTestCoreToolsDir, true)

        val funcCoreToolApp = azureTestCoreToolsDir.resolve(FunctionsCoreToolsManager.getCoreToolsExecutableName())
        funcCoreToolApp.setExecutable(false)

        val versionValue =
                FunctionsCoreToolsManager.determineVersion(azureTestCoreToolsDir.path)

        val version = versionValue.shouldNotBeNull()
        version.version.shouldBe("3.0.2009")
    }

    //endregion determineVersion()

    //region determineLatestLocalCoreToolsPath()

    @Test
    fun testDetermineLatestLocalCoreToolsPath_NotExists() {
        val version = FunctionsCoreToolsManager.determineLatestLocalCoreToolsPath()
        version.shouldBeNull("Version for non-existing function core tools directory must return NULL.")
    }

    @Test
    fun testDetermineLatestLocalCoreToolsPath_NotInstalled() {
        val version = FunctionsCoreToolsManager.determineLatestLocalCoreToolsPath(
                toolsDownloadPath = azureTestCoreToolsDir.path)

        version.shouldBeNull("Latest version for non-installed function core tools must return NULL.")
    }

    @Test
    fun testDetermineLatestLocalCoreToolsPath_SingleVersion() {
        prepareCoreToolsDirs(azureTestCoreToolsDir, arrayOf("3.0.2358"))

        val version = FunctionsCoreToolsManager.determineLatestLocalCoreToolsPath(
                toolsDownloadPath = azureTestCoreToolsDir.path)

        val toolsVersion = version.shouldNotBeNull("Latest version for an existing function core tools directory must return a valid version.")
        toolsVersion.shouldBe(azureTestCoreToolsDir.resolve("3.0.2358").path)
    }

    @Test
    fun testDetermineLatestLocalCoreToolsPath_MultipleVersions() {
        prepareCoreToolsDirs(azureTestCoreToolsDir, arrayOf("3.0.2358", "3.0.2222", "2.0.0001"))

        val version = FunctionsCoreToolsManager.determineLatestLocalCoreToolsPath(
                toolsDownloadPath = azureTestCoreToolsDir.path)

        val toolsVersion = version.shouldNotBeNull("Latest version for an existing function core tools directory must return a valid version.")
        toolsVersion.shouldBe(azureTestCoreToolsDir.resolve("3.0.2358").path)
    }

    //endregion determineLatestLocalCoreToolsPath()

    //region determineLatestRemote()

    @Test(enabled = false)
    fun testDetermineLatestRemote_AllowPreRelease_ValidLatestRemote() {
        //FunctionsCoreToolsManager.determineLatestRemote(allowPrerelease = false)
        // TODO: This is better to test with an integration test having [GitHubReleaseService] as a service with test implementation
        true.shouldBeFalse()
    }

    @Test(enabled = false)
    fun testDetermineLatestRemote_DoNotAllowPreRelease_ValidLatestRemote() {
        //FunctionsCoreToolsManager.determineLatestRemote(allowPrerelease = false)
        // TODO: This is better to test with an integration test having [GitHubReleaseService] as a service with test implementation
        true.shouldBeFalse()
    }

    //endregion determineLatestRemote()

    private fun prepareCoreToolsDirs(coreToolsDir: File, versions: Array<String>) {
        versions.forEach { version ->
            val versionDir = coreToolsDir.resolve(version)
            versionDir.mkdirs().shouldBeTrue("Failed to create folder with name: ${versionDir.canonicalPath}")
        }
    }
}
