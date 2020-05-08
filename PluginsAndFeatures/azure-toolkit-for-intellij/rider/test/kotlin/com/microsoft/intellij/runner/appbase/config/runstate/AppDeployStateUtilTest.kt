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

package com.microsoft.intellij.runner.appbase.config.runstate

import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rdclient.util.idea.toIOFile
import com.jetbrains.rider.model.PublishableProjectOutput
import com.jetbrains.rider.test.asserts.shouldBe
import com.jetbrains.rider.test.asserts.shouldBeTrue
import com.jetbrains.rider.test.asserts.shouldNotBe
import com.microsoft.intellij.runner.RunProcessHandler
import org.jetbrains.mock.FunctionAppMock
import org.jetbrains.mock.PublishableProjectMock
import org.jetbrains.mock.SqlDatabaseMock
import org.jetbrains.mock.WebAppMock
import org.testng.annotations.AfterMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File
import java.io.FileNotFoundException

class AppDeployStateUtilTest {

    private val tempDir: File by lazy {
        val dir = FileUtil.getTempDirectory().toIOFile()
        val dirToCreate = dir.resolve(javaClass.simpleName)
        if (!dirToCreate.exists()) {
            dirToCreate.mkdirs()
            dirToCreate.deleteOnExit()
        }
        dirToCreate
    }

    @AfterMethod(alwaysRun = true)
    fun cleanupWorkingDir() {
        if (tempDir.exists())
            tempDir.listFiles().orEmpty().forEach { it.deleteRecursively() }
    }

    //region Connection String

    @Test
    fun testGenerateConnectionString_ValidString() {
        val mockDatabase = SqlDatabaseMock(name = "test-sql-database", sqlServerName = "test-sql-server")
        val connectionString = AppDeployStateUtil.generateConnectionString(
                fullyQualifiedDomainName = "test.name", database = mockDatabase, adminLogin = "user", adminPassword = "password".toCharArray())

        connectionString.shouldBe(
                "Data Source=tcp:test.name,1433;Initial Catalog=${mockDatabase.name()};" +
                        "User Id=user@${mockDatabase.sqlServerName()};Password=password")
    }

    //endregion Connection String

    //region Get App URL

    @Test
    fun testGetAppUrl_WebAppUrl() {
        val mockWebApp = WebAppMock(defaultHostName = "test-host-name")
        val url = AppDeployStateUtil.getAppUrl(mockWebApp)

        url.shouldBe("https://test-host-name")
    }

    @Test
    fun testGetAppUrl_FunctionAppUrl() {
        val mockFunctionApp = FunctionAppMock(defaultHostName = "test-host-name")
        val url = AppDeployStateUtil.getAppUrl(mockFunctionApp)

        url.shouldBe("https://test-host-name")
    }

    //endregion Get App URL

    //region Get Assembly Relative Path

    data class RelativePathArtifactsData(
            val projectName: String, val assemblyName: String, val relativeDir: File, val expectedPath: String)

    @DataProvider(name = "getAssemblyRelativePathArtifactsData")
    fun getAssemblyRelativePathArtifactsData() = arrayOf(
            arrayOf("RootDir", RelativePathArtifactsData(
                    projectName = "TestProject",
                    assemblyName = "TestProject.dll",
                    relativeDir = File("."),
                    expectedPath = "TestProject.dll")),

            arrayOf("RelativeDir", RelativePathArtifactsData(
                    projectName = "TestProject",
                    assemblyName = "TestProject.dll",
                    relativeDir = File("relative"),
                    expectedPath = File("relative", "TestProject.dll").path)),

            arrayOf("DifferentAssemblyName", RelativePathArtifactsData(
                    projectName = "TestProject",
                    assemblyName = "TestProjectAssembly.dll",
                    relativeDir = File("."),
                    expectedPath = "TestProjectAssembly.dll"))
    )

    @Test(dataProvider = "getAssemblyRelativePathArtifactsData")
    fun testGetAssemblyRelativePath_Path(name: String, relativePathData: RelativePathArtifactsData) {
        val projectName = relativePathData.projectName
        val assemblyName = relativePathData.assemblyName

        val projectArtifactsDir = tempDir.resolve("RiderTestProjectArtifacts")
        projectArtifactsDir.mkdirs()

        val artifactRelativeDir = projectArtifactsDir.resolve(relativePathData.relativeDir)
        artifactRelativeDir.mkdirs()

        val artifactsAssemblyFile = artifactRelativeDir.resolve(assemblyName)
        artifactsAssemblyFile.createNewFile()

        val tfm = "netcoreapp3.1"
        val projectOutputs = tempDir
                .resolve("RiderTestProjectOutputs")
                .resolve("bin")
                .resolve("Debug")
                .resolve(tfm)
        projectOutputs.mkdirs()

        val projectExeFile = projectOutputs.resolve(assemblyName)
        projectExeFile.createNewFile()

        val relativePath = AppDeployStateUtil.getAssemblyRelativePath(
                publishableProject = PublishableProjectMock.createMockProject(
                        name = projectName,
                        projectOutputs = listOf(PublishableProjectOutput(
                                tfm = tfm,
                                tfmInMsbuildFormat = "",
                                exePath = projectExeFile.canonicalPath
                        ))
                ),
                outDir = projectArtifactsDir
        )

        relativePath.shouldBe(relativePathData.expectedPath)
    }

    @Test
    fun testGetAssemblyRelativePath_MultipleOutputDirs() {
        val projectName = "TestProject"
        val assemblyName = "TestProjectAssembly.dll"

        val projectArtifactsDir = tempDir.resolve("RiderTestProjectArtifacts")
        projectArtifactsDir.mkdirs()

        val artifactsAssemblyFile = projectArtifactsDir.resolve(assemblyName)
        artifactsAssemblyFile.createNewFile()

        val projectOutputsOne = tempDir
                .resolve("RiderTestProjectOutputs")
                .resolve("bin")
                .resolve("Debug")
                .resolve("netcoreapp2.0")
        projectOutputsOne.mkdirs()

        val projectExeFileOne = projectOutputsOne.resolve(assemblyName)

        val projectOutputsTwo = tempDir
                .resolve("RiderTestProjectOutputs")
                .resolve("bin")
                .resolve("Debug")
                .resolve("netcoreapp3.1")
        projectOutputsTwo.mkdirs()

        val projectExeFileTwo = projectOutputsTwo.resolve(assemblyName)
        projectExeFileTwo.createNewFile()

        val relativePath = AppDeployStateUtil.getAssemblyRelativePath(
                publishableProject = PublishableProjectMock.createMockProject(
                        name = projectName,
                        projectOutputs = listOf(
                                PublishableProjectOutput(
                                        tfm = "netcoreapp2.0",
                                        tfmInMsbuildFormat = "",
                                        exePath = projectExeFileOne.canonicalPath
                                ),
                                PublishableProjectOutput(
                                        tfm = "netcoreapp3.1",
                                        tfmInMsbuildFormat = "",
                                        exePath = projectExeFileTwo.canonicalPath
                                )
                        )
                ),
                outDir = projectArtifactsDir
        )

        relativePath.shouldBe(assemblyName)
    }

    @Test
    fun testGetAssemblyRelativePath_OutputDirIsEmpty() {
        val projectName = "TestProject"
        val assemblyName = "TestProjectAssembly.dll"

        val projectArtifactsDir = tempDir.resolve("RiderTestProjectArtifacts")
        projectArtifactsDir.mkdirs()

        val artifactsAssemblyFile = projectArtifactsDir.resolve(assemblyName)
        artifactsAssemblyFile.createNewFile()

        val tfm = "netcoreapp3.1"
        val projectOutputs = tempDir
                .resolve("RiderTestProjectOutputs")
                .resolve("bin")
                .resolve("Debug")
                .resolve(tfm)
        projectOutputs.mkdirs()

        val projectExeFile = projectOutputs.resolve(assemblyName)

        val relativePath = AppDeployStateUtil.getAssemblyRelativePath(
                publishableProject = PublishableProjectMock.createMockProject(
                        name = projectName,
                        projectOutputs = listOf(PublishableProjectOutput(
                                tfm = tfm,
                                tfmInMsbuildFormat = "",
                                exePath = projectExeFile.canonicalPath
                        ))
                ),
                outDir = projectArtifactsDir
        )

        relativePath.shouldBe("$projectName.dll")
    }

    //endregion Get Assembly Relative Path

    //region Zip Project Artifacts

    @Test(expectedExceptions = [FileNotFoundException::class], expectedExceptionsMessageRegExp = "Original file '.+TestProjectArtifacts' not found")
    fun testZipProjectArtifacts_FileNotExist() {
        val projectArtifacts = tempDir.resolve("TestProjectArtifacts")
        val processHandler = RunProcessHandler()
        AppDeployStateUtil.zipProjectArtifacts(
                fromFile = projectArtifacts, processHandler = processHandler, deleteOriginal = true)
    }

    @Test()
    fun testZipProjectArtifacts_FileExists() {
        val projectArtifacts = tempDir.resolve("TestProjectArtifacts")
        projectArtifacts.mkdirs()

        val processHandler = RunProcessHandler()
        val zipFile = AppDeployStateUtil.zipProjectArtifacts(
                fromFile = projectArtifacts, processHandler = processHandler)

        zipFile.exists().shouldBeTrue()
        zipFile.extension.shouldBe("zip")
        zipFile.readBytes().shouldNotBe(ByteArray(0))
    }

    @DataProvider(name = "zipProjectArtifactsDeleteOriginalData")
    fun zipProjectArtifactsDeleteOriginalData() = arrayOf(
            arrayOf("Delete", true, false),
            arrayOf("NotDelete", false, true)
    )

    @Test(dataProvider = "zipProjectArtifactsDeleteOriginalData")
    fun testZipProjectArtifacts_DeleteOriginal(name: String, deleteOriginal: Boolean, exists: Boolean) {
        val projectArtifacts = tempDir.resolve("TestProjectArtifacts")
        projectArtifacts.mkdirs()

        val processHandler = RunProcessHandler()
        val zipFile = AppDeployStateUtil.zipProjectArtifacts(
                fromFile = projectArtifacts, processHandler = processHandler, deleteOriginal = deleteOriginal)

        zipFile.exists().shouldBeTrue()
        projectArtifacts.exists().shouldBe(exists)
    }

    //endregion Zip Project Artifacts
}
