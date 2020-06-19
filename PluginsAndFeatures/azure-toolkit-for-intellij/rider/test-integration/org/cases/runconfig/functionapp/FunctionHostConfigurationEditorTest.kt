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

import com.intellij.openapi.util.Disposer
import com.jetbrains.rider.model.RunnableProjectKind
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.test.annotations.TestEnvironment
import com.jetbrains.rider.test.asserts.*
import com.jetbrains.rider.test.base.BaseTestWithSolution
import com.jetbrains.rider.test.enums.CoreVersion
import com.jetbrains.rider.test.scriptingApi.createRunConfiguration
import org.jetbrains.plugins.azure.functions.run.AzureFunctionsHostConfiguration
import org.jetbrains.plugins.azure.functions.run.AzureFunctionsHostConfigurationEditor
import org.jetbrains.plugins.azure.functions.run.AzureFunctionsHostConfigurationType
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

@TestEnvironment(coreVersion = CoreVersion.DEFAULT)
class FunctionHostConfigurationEditorTest : BaseTestWithSolution() {

    override fun getSolutionDirectoryName(): String = "FunctionApp"

    override val waitForCaches: Boolean = true

    lateinit var editor: AzureFunctionsHostConfigurationEditor

    @BeforeMethod
    fun setUp() {
        editor = AzureFunctionsHostConfigurationEditor(project)
    }

    @AfterMethod
    fun tearDown() {
        Disposer.dispose(editor)
    }

    private val type = AzureFunctionsHostConfigurationType()

    @Test
    fun testFunctionHost_DefaultParameters() {
        val configuration = createRunConfiguration(
                name = "Run FunctionApp",
                configurationType = AzureFunctionsHostConfigurationType::class.java
        ) as AzureFunctionsHostConfiguration

        editor.component // to initialize viewModel
        editor.resetFrom(configuration)
        editor.applyTo(configuration)

        val runnableProject =
                project.solution.runnableProjectsModel.projects.valueOrNull?.find { it.name == "FunctionApp" }.shouldNotBeNull()

        val parameters = configuration.parameters
        parameters.project.name.shouldBe("FunctionApp")
        parameters.projectFilePath.shouldBe(runnableProject.projectFilePath)
        parameters.functionNames.shouldBeEmpty()
        parameters.isUnloadedProject.shouldBeFalse()
        parameters.projectKind.shouldBe(RunnableProjectKind.AzureFunctions)
        parameters.projectTfm.shouldBe(".NETCoreApp,Version=v3.1")

        val projectOutput = runnableProject.projectOutputs.first()
        parameters.workingDirectory.shouldBe(projectOutput.workingDirectory)
        parameters.exePath.shouldBe(projectOutput.exePath)

        parameters.programParameters.shouldBe("host start --port 7071 --pause-on-error")
        parameters.envs.size.shouldBe(0)
        parameters.isPassParentEnvs.shouldBeTrue()
        parameters.useExternalConsole.shouldBeFalse()

        parameters.startBrowserParameters.browser.shouldBeNull()
        parameters.startBrowserParameters.startAfterLaunch.shouldBeFalse()
        parameters.startBrowserParameters.url.shouldBe("http://localhost:7071")
        parameters.startBrowserParameters.withJavaScriptDebugger.shouldBeFalse()

        parameters.trackProjectArguments.shouldBeTrue()
        parameters.trackProjectExePath.shouldBeTrue()
        parameters.trackProjectWorkingDirectory.shouldBeTrue()
    }
}
