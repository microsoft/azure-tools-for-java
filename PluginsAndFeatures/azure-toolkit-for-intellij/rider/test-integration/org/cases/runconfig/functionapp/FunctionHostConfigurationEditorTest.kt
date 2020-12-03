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

package org.cases.runconfig.functionapp

import com.intellij.openapi.util.Disposer
import com.jetbrains.rider.model.RunnableProjectKind
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.projectView.solutionDirectory
import com.jetbrains.rider.run.configurations.controls.ProgramParametersEditor
import com.jetbrains.rider.run.configurations.controls.TextEditor
import com.jetbrains.rider.test.annotations.TestEnvironment
import com.jetbrains.rider.test.asserts.*
import com.jetbrains.rider.test.base.BaseTestWithSolution
import com.jetbrains.rider.test.enums.CoreVersion
import com.jetbrains.rider.test.scriptingApi.changeFileContent
import com.jetbrains.rider.test.scriptingApi.createRunConfiguration
import org.jetbrains.plugins.azure.RiderAzureBundle
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

    @Test(description = "Read default parameters for function host run configuration.")
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

        parameters.programParameters.shouldBe("host start --pause-on-error")
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

    @Test(description = "Read port from command arguments first and use it in URL text field.")
    fun testFunctionHost_PortInCommandlineArguments() {
        val configuration = createRunConfiguration(
            name = "Run FunctionApp",
            configurationType = AzureFunctionsHostConfigurationType::class.java
        ) as AzureFunctionsHostConfiguration

        editor.component // to initialize viewModel
        editor.resetFrom(configuration)
        editor.applyTo(configuration)

        val controls = editor.viewModel.controls
        val browserUrlEditor = controls.filterIsInstance<TextEditor>()
            .find { it.name == RiderAzureBundle.message("run_config.run_function_app.form.function_app.url") }
            .shouldNotBeNull()
        browserUrlEditor.text.value.shouldBe("http://localhost:7071")

        val programParameters = controls.filterIsInstance<ProgramParametersEditor>().single()
        programParameters.parametersString.set("host start --port 7072 --pause-on-error")

        browserUrlEditor.text.value.shouldBe("http://localhost:7072")
    }

    // Note: We copy a new solution instance before a test in [org.cases.runconfig.functionapp.BaseTestWithSolution]
    //  This means - we do not need to reset all changes in local.settings.json file.
    @Test(description = "Read port value from local.settings.json file and set URL text field with a proper port value.")
    fun testFunctionHost_PortInLocalSettingsJson() {
        prepareLocalSettingsFile()

        val configuration = createRunConfiguration(
            name = "Run FunctionApp",
            configurationType = AzureFunctionsHostConfigurationType::class.java
        ) as AzureFunctionsHostConfiguration

        editor.component // to initialize viewModel
        editor.resetFrom(configuration)
        editor.applyTo(configuration)

        val controls = editor.viewModel.controls
        val browserUrlEditor = controls.filterIsInstance<TextEditor>()
                .find { it.name == RiderAzureBundle.message("run_config.run_function_app.form.function_app.url") }
                .shouldNotBeNull()
        browserUrlEditor.text.value.shouldBe("http://localhost:7072")
    }

    @Test(description = "Check the case when port is specified in both command arguments in configuration and in " +
            "local.settings.json file. Command arguments port value wins the fight.")
    fun testFunctionHost_PortInCommandLineAndLocalSettings() {
        prepareLocalSettingsFile()

        val configuration = createRunConfiguration(
                name = "Run FunctionApp",
                configurationType = AzureFunctionsHostConfigurationType::class.java
        ) as AzureFunctionsHostConfiguration

        editor.component // to initialize viewModel
        editor.resetFrom(configuration)
        editor.applyTo(configuration)

        val controls = editor.viewModel.controls
        val browserUrlEditor = controls.filterIsInstance<TextEditor>()
                .find { it.name == RiderAzureBundle.message("run_config.run_function_app.form.function_app.url") }
                .shouldNotBeNull()

        val programParameters = controls.filterIsInstance<ProgramParametersEditor>().single()
        programParameters.parametersString.set("host start --port 7073 --pause-on-error")

        browserUrlEditor.text.value.shouldBe("http://localhost:7073")
    }

    private fun prepareLocalSettingsFile() {
        val source = testCaseSourceDirectory.resolve("local.settings.json")
        if (!source.exists()) return

        val localSettingFile =
                project.solutionDirectory.resolve("FunctionApp").resolve("local.settings.json")

        changeFileContent(project, localSettingFile) {
            source.readText()
        }
    }
}
