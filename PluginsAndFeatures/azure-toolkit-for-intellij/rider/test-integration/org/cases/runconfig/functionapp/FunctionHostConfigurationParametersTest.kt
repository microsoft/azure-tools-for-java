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

package org.cases.runconfig.functionapp

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.jetbrains.rd.ide.model.EnvironmentVariable
import com.jetbrains.rider.model.*
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.project.DotNetStartBrowserParameters
import com.jetbrains.rider.runtime.RiderDotNetActiveRuntimeHost
import com.jetbrains.rider.runtime.mono.MonoRuntime
import com.jetbrains.rider.test.annotations.TestEnvironment
import com.jetbrains.rider.test.base.BaseTestWithSolution
import com.jetbrains.rider.test.enums.CoreVersion
import com.microsoft.intellij.configuration.AzureRiderSettings
import org.jetbrains.plugins.azure.functions.run.AzureFunctionsHostConfigurationParameters
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File

@TestEnvironment(coreVersion = CoreVersion.DEFAULT)
class FunctionHostConfigurationParametersTest : BaseTestWithSolution() {

    override fun getSolutionDirectoryName(): String = "FunctionApp"

    override val waitForCaches: Boolean = true

    override val restoreNuGetPackages: Boolean = true

    private var originalCoreToolsProperty: String? = null

    @BeforeMethod(alwaysRun = true)
    fun collectCoreToolsProperties() {
        originalCoreToolsProperty =
                PropertiesComponent.getInstance().getValue(AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_PATH)
    }

    @AfterMethod(alwaysRun = true)
    fun restoreCoreToolsProperty() {
        PropertiesComponent.getInstance().setValue(
                AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_PATH, originalCoreToolsProperty)
    }

    //region Invalid

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Solution is loading, please wait for a few seconds\\.")
    fun testValidate_Solution_NotLoaded() {
        val parameters = createParameters(project = project)
        project.solution.isLoaded.set(false)
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Project is not specified\\.")
    fun testValidate_RunnableProjects_NoRunnableProjects() {
        val parameters = createParameters(project = project)
        project.solution.runnableProjectsModel.projects.set(emptyList())
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Project is not specified\\.")
    fun testValidate_RunnableProjects_NoFunctionProjectType() {
        val parameters = createParameters(project = project)
        project.solution.runnableProjectsModel.projects.set(listOf(createRunnableProject(kind = RunnableProjectKind.DotNetCore)))
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Project is not specified\\.")
    fun testValidate_RunnableProjects_MultipleRunnableProjectsMatch() {
        val projectFilePath = File("/project/file/path").absolutePath
        val parameters = createParameters(project = project, projectFilePath = projectFilePath)

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(kind = RunnableProjectKind.DotNetCore, projectFilePath = projectFilePath),
                createRunnableProject(kind = RunnableProjectKind.DotNetCore, projectFilePath = projectFilePath)
        ))
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Function App is not loaded properly\\.")
    fun testValidate_RunnableProjects_ProjectWithProblems() {
        val projectFilePath = File("/project/file/path").absolutePath
        val parameters = createParameters(project = project, projectFilePath = projectFilePath)
        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = RunnableProjectKind.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = "Function App is not loaded properly."
                )
        ))
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Invalid exe path: '/not/existing/path'\\.")
    fun testValidate_TrackProjectExePath_NotExists() {
        val projectFilePath = File("/project/file/path").absolutePath

        val parameters =
                createParameters(project = project, projectFilePath = projectFilePath, trackProjectExePath = false, exePath = "/not/existing/path")

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = RunnableProjectKind.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = null
                )
        ))
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Invalid exe path: '<empty>'\\.")
    fun testValidate_TrackProjectExePath_EmptyExePath() {
        val projectFilePath = File("/project/file/path").absolutePath

        val parameters =
                createParameters(project = project, projectFilePath = projectFilePath, trackProjectExePath = false, exePath = "")

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = RunnableProjectKind.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = null
                )
        ))
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Invalid exe path:\\s.+\\.")
    fun testValidate_TrackProjectExePath_NotAFile() {
        val projectFilePath = File("/project/file/path").absolutePath

        val parameters = createParameters(
                project = project,
                projectFilePath = projectFilePath,
                trackProjectExePath = false,
                exePath = tempTestDirectory.path
        )

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = RunnableProjectKind.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = null
                )
        ))
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Invalid working directory: '/not/existing/path'\\.")
    fun testValidate_TrackProjectWorkingDirectory_NotExists() {
        val projectFilePath = File("/project/file/path").absolutePath

        val parameters = createParameters(
                project = project,
                projectFilePath = projectFilePath,
                trackProjectExePath = true,
                trackProjectWorkingDirectory = false,
                workingDirectory = "/not/existing/path"
        )

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = RunnableProjectKind.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = null
                )
        ))
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Invalid working directory: '<empty>'\\.")
    fun testValidate_TrackProjectWorkingDirectory_EmptyDirectory() {
        val projectFilePath = File("/project/file/path").absolutePath

        val parameters = createParameters(
                project = project,
                projectFilePath = projectFilePath,
                trackProjectExePath = true,
                trackProjectWorkingDirectory = false,
                workingDirectory = ""
        )

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = RunnableProjectKind.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = null
                )
        ))
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Invalid working directory:\\s.+\\.")
    fun testValidate_TrackProjectWorkingDirectory_NotADirectory() {
        val projectFilePath = File("/project/file/path").absolutePath

        val workingDirectoryFile = tempTestDirectory.resolve("FunctionApp.dll")
        workingDirectoryFile.createNewFile()

        val parameters = createParameters(
                project = project,
                projectFilePath = projectFilePath,
                trackProjectExePath = true,
                trackProjectWorkingDirectory = false,
                workingDirectory = workingDirectoryFile.path
        )

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = RunnableProjectKind.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = null
                )
        ))
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Path to Azure Functions core tools has not been configured\\. This can be done in the settings under Tools \\| Azure \\| Functions\\.")
    fun testValidate_FunctionCoreTools_NotInstalled() {
        val projectFilePath = File("/project/file/path").absolutePath

        val parameters = createParameters(
                project = project,
                projectFilePath = projectFilePath,
                trackProjectExePath = true,
                trackProjectWorkingDirectory = true
        )

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = RunnableProjectKind.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = null
                )
        ))

        PropertiesComponent.getInstance().unsetValue(AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_PATH)
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Mono runtime not found\\. Please setup Mono path in settings \\(File \\| Settings \\| Build, Execution, Deployment \\| Toolset and Build\\)")
    fun testValidate_MonoRuntime_MissingMonoConfig() {
        val projectFilePath = File("/project/file/path").absolutePath

        val parameters = createParameters(
                project = project,
                projectFilePath = projectFilePath,
                trackProjectExePath = true,
                trackProjectWorkingDirectory = true
        ).apply { useMonoRuntime = true }

        val host = createHost(monoRuntime = null)

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = RunnableProjectKind.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = null)
        ))

        setCoreToolsPath()
        parameters.validate(host)
    }

    //endregion Invalid

    //region Valid

    @Test
    fun testValidate_Valid_UseMonoRuntime() {
        val projectFilePath = File("/project/file/path").absolutePath

        val parameters = createParameters(
                project = project,
                projectFilePath = projectFilePath,
                trackProjectExePath = true,
                trackProjectWorkingDirectory = true
        ).apply { useMonoRuntime = true }

        val host = createHost(monoRuntime = MonoRuntime(monoExePath = ""))

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = RunnableProjectKind.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = null)
        ))

        setCoreToolsPath()
        parameters.validate(host)
    }

    @Test
    fun testValidate_Valid_NetCoreRuntime() {
        val projectFilePath = File("/project/file/path").absolutePath

        val parameters = createParameters(
                project = project,
                projectFilePath = projectFilePath,
                trackProjectExePath = true,
                trackProjectWorkingDirectory = true
        ).apply { useMonoRuntime = false }

        val host = createHost()

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = RunnableProjectKind.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = null)
        ))

        setCoreToolsPath()
        parameters.validate(host)
    }

    //endregion Valid

    private fun createParameters(
            project: Project = this.project,
            exePath: String = "",
            programParameters: String = "host start --pause-on-error",
            workingDirectory: String = File("/working/path").absolutePath,
            envs: Map<String, String> = emptyMap(),
            isPassParentEnvs: Boolean = true,
            useExternalConsole: Boolean = false,
            projectFilePath: String = "",
            trackProjectExePath: Boolean = false,
            trackProjectArguments: Boolean = false,
            trackProjectWorkingDirectory: Boolean = false,
            projectKind: RunnableProjectKind = RunnableProjectKind.AzureFunctions,
            projectTfm: String = ".NETCoreApp,Version=v3.1",
            functionNames: String = "",
            startBrowserParameters: DotNetStartBrowserParameters = DotNetStartBrowserParameters()
    ): AzureFunctionsHostConfigurationParameters =
            AzureFunctionsHostConfigurationParameters(
                    project = project,
                    exePath = exePath,
                    programParameters = programParameters,
                    workingDirectory = workingDirectory,
                    envs = envs,
                    isPassParentEnvs = isPassParentEnvs,
                    useExternalConsole = useExternalConsole,
                    projectFilePath = projectFilePath,
                    trackProjectExePath = trackProjectExePath,
                    trackProjectArguments = trackProjectArguments,
                    trackProjectWorkingDirectory = trackProjectWorkingDirectory,
                    projectKind = projectKind,
                    projectTfm = projectTfm,
                    functionNames = functionNames,
                    startBrowserParameters = startBrowserParameters
            )

    fun createHost(monoRuntime: MonoRuntime? = null): RiderDotNetActiveRuntimeHost {
        val host = RiderDotNetActiveRuntimeHost(project)
        host.monoRuntime = monoRuntime

        return host
    }

    fun createRunnableProject(
            name: String = "TestName",
            fullName: String = "TestFullName",
            projectFilePath: String = File("/project/file/path").absolutePath,
            kind: RunnableProjectKind = RunnableProjectKind.AzureFunctions,
            projectOutputs: List<ProjectOutput> = emptyList(),
            environmentVariables: List<EnvironmentVariable> = emptyList(),
            problems: String? = null,
            customAttributes: List<CustomAttribute> = emptyList()
    ): RunnableProject =
        RunnableProject(
                name = name,
                fullName = fullName,
                projectFilePath = projectFilePath,
                kind = kind,
                projectOutputs = projectOutputs,
                environmentVariables = environmentVariables,
                problems = problems,
                customAttributes = customAttributes
        )

    private fun setCoreToolsPath(): File {
        val funcCoreTools = tempTestDirectory.resolve("func")
        funcCoreTools.createNewFile()
        funcCoreTools.setExecutable(true)

        PropertiesComponent.getInstance().setValue(AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_PATH, tempTestDirectory.canonicalPath)

        return funcCoreTools
    }
}
