/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.SequentialLifetimes
import com.jetbrains.rdclient.protocol.IPermittedModalities
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.controls.*
import com.jetbrains.rider.run.configurations.controls.startBrowser.BrowserSettingsEditor
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import javax.swing.JComponent

class AzureFunctionsHostConfigurationEditor(private val project: Project)
    : SettingsEditor<AzureFunctionsHostConfiguration>() {

    lateinit var viewModel: AzureFunctionsHostConfigurationViewModel
    private val lifetimeDefinition = Lifetime.Eternal.createNested()
    private val editorLifetime = SequentialLifetimes(lifetimeDefinition.lifetime)

    override fun disposeEditor() {
        lifetimeDefinition.terminate()
        super.disposeEditor()
    }

    override fun resetEditorFrom(configuration: AzureFunctionsHostConfiguration) {
        configuration.parameters.apply {
            viewModel.reset(
                    projectFilePath,
                    trackProjectExePath,
                    trackProjectArguments,
                    trackProjectWorkingDirectory,
                    projectTfm,
                    exePath,
                    programParameters,
                    workingDirectory,
                    functionNames,
                    envs,
                    isPassParentEnvs,
                    useExternalConsole,
                    isUnloadedProject,
                    startBrowserParameters
            )
        }
    }

    override fun applyEditorTo(configuration: AzureFunctionsHostConfiguration) {
        val selectedProject = viewModel.projectSelector.project.valueOrNull
        val selectedTfm = viewModel.tfmSelector.string.valueOrNull
        if (selectedProject != null) {
            configuration.parameters.projectFilePath = selectedProject.projectFilePath
            if (selectedTfm != null) {
                configuration.parameters.apply {
                    projectKind = selectedProject.kind
                    trackProjectExePath = viewModel.trackProjectExePath
                    trackProjectArguments = viewModel.trackProjectArguments
                    trackProjectWorkingDirectory = viewModel.trackProjectWorkingDirectory
                    projectTfm = selectedTfm
                    functionNames = viewModel.functionNamesEditor.text.value
                    exePath = FileUtil.toSystemIndependentName(viewModel.exePathSelector.path.value)
                    programParameters = viewModel.programParametersEditor.parametersString.value
                    workingDirectory = FileUtil.toSystemIndependentName(viewModel.workingDirectorySelector.path.value)
                    envs = viewModel.environmentVariablesEditor.envs.value
                    isPassParentEnvs = viewModel.environmentVariablesEditor.isPassParentEnvs.value
                    useExternalConsole = viewModel.useExternalConsoleEditor.isSelected.value

                    startBrowserParameters.url = viewModel.urlEditor.text.value
                    startBrowserParameters.browser = viewModel.dotNetBrowserSettingsEditor.settings.value.myBrowser
                    startBrowserParameters.startAfterLaunch = viewModel.dotNetBrowserSettingsEditor.settings.value.startAfterLaunch
                    startBrowserParameters.withJavaScriptDebugger = viewModel.dotNetBrowserSettingsEditor.settings.value.withJavaScriptDebugger
                }
            }
        }
    }

    override fun createEditor(): JComponent {
        IPermittedModalities.getInstance().allowPumpProtocolUnderCurrentModality()

        val lifetime = editorLifetime.next()
        viewModel = AzureFunctionsHostConfigurationViewModel(
                lifetime,
                project.solution.runnableProjectsModel,
                ProjectSelector(message("run_config.common.form.project_label")),
                StringSelector(message("run_config.common.form.target_framework_label")),
                ProgramParametersEditor(message("run_config.run.form.function_app.function_host_args_label"), lifetime),
                PathSelector(message("run_config.run.form.function_app.working_directory_label"), FileChooserDescriptorFactory.createSingleFolderDescriptor(), lifetime),
                TextEditor(message("run_config.run.form.function_app.function_names_to_run_label"), lifetime),
                EnvironmentVariablesEditor(message("run_config.common.form.environment_variables")),
                FlagEditor(message("run_config.run.form.function_app.use_external_console")),
                ViewSeparator(message("run_config.run.form.function_app.open_browser")),
                TextEditor(message("run_config.run.form.function_app.url"), lifetime),
                BrowserSettingsEditor("")
        )
        return ControlViewBuilder(lifetime, project).build(viewModel)
    }
}