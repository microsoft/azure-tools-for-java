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

import com.intellij.openapi.project.Project
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.adviseOnce
import com.jetbrains.rider.model.EnvironmentVariable
import com.jetbrains.rider.model.Key
import com.jetbrains.rider.model.ProjectOutput
import com.jetbrains.rider.model.RunnableProjectsModel
import com.jetbrains.rider.model.RunnableProjectKind
import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.run.configurations.controls.*
import com.jetbrains.rider.run.configurations.controls.startBrowser.BrowserSettings
import com.jetbrains.rider.run.configurations.controls.startBrowser.BrowserSettingsEditor
import com.jetbrains.rider.run.configurations.dotNetExe.DotNetExeConfigurationViewModel
import com.jetbrains.rider.run.configurations.project.DotNetStartBrowserParameters
import org.apache.http.client.utils.URIBuilder
import java.io.File

class AzureFunctionsHostConfigurationViewModel(
        private val lifetime: Lifetime,
        project: Project,
        private val runnableProjectsModel: RunnableProjectsModel,
        val projectSelector: ProjectSelector,
        val tfmSelector: StringSelector,
        programParametersEditor: ProgramParametersEditor,
        workingDirectorySelector: PathSelector,
        val functionNamesEditor: TextEditor,
        environmentVariablesEditor: EnvironmentVariablesEditor,
        useExternalConsoleEditor: FlagEditor,
        separator: ViewSeparator,
        val urlEditor: TextEditor,
        val dotNetBrowserSettingsEditor: BrowserSettingsEditor
) : DotNetExeConfigurationViewModel(
        lifetime,
        project,
        PathSelector("", null, lifetime),
        programParametersEditor,
        workingDirectorySelector,
        environmentVariablesEditor,
        FlagEditor(""),
        ProgramParametersEditor("", lifetime),
        false,
        useExternalConsoleEditor) {

    override val controls: List<ControlBase> = listOf(
            projectSelector,
            tfmSelector,
            programParametersEditor,
            workingDirectorySelector,
            functionNamesEditor,
            environmentVariablesEditor,
            useExternalConsoleEditor,
            separator,
            urlEditor,
            dotNetBrowserSettingsEditor
    )

    private var isLoaded = false
    private val type = AzureFunctionsHostConfigurationType()

    var trackProjectExePath = true
    var trackProjectArguments = true
    var trackProjectWorkingDirectory = true

    private val portRegex = Regex("(--port|-p) (\\d+)", RegexOption.IGNORE_CASE)

    init {
        disable()

        projectSelector.bindTo(
                runnableProjectsModel = runnableProjectsModel,
                lifetime = lifetime,
                projectFilter = { p: RunnableProject -> type.isApplicable(p.kind) },
                onLoad = ::enable,
                onSelect = ::handleProjectSelection
        )

        tfmSelector.string.advise(lifetime) { handleChangeTfmSelection() }
        exePathSelector.path.advise(lifetime) { recalculateTrackProjectOutput() }
        programParametersEditor.parametersString.advise(lifetime) { recalculateTrackProjectOutput() }
        workingDirectorySelector.path.advise(lifetime) { recalculateTrackProjectOutput() }

        // Please make sure it is executed after you enable controls through [RunConfigurationViewModelBase::enable]
        disableBrowserUrl()
    }

    private fun disableBrowserUrl() {
        urlEditor.isEnabled.set(false)
    }

    private fun handleChangeTfmSelection() {
        projectSelector.project.valueOrNull?.projectOutputs
                ?.singleOrNull { it.tfm == tfmSelector.string.valueOrNull }
                ?.let { projectOutput ->
                    val shouldChangeExePath = trackProjectExePath
                    val shouldChangeWorkingDirectory = trackProjectWorkingDirectory
                    if (shouldChangeExePath) {
                        exePathSelector.path.set(projectOutput.exePath)
                    }
                    if (shouldChangeWorkingDirectory) {
                        workingDirectorySelector.path.set(projectOutput.workingDirectory)
                    }
                    exePathSelector.defaultValue.set(projectOutput.exePath)

                    // Ensure we have the defaults if needed...
                    val patchedProjectOutput = AzureFunctionsRunnableProjectUtil.patchProjectOutput(projectOutput)

                    // ...but keep the previous value if it's not empty
                    if (programParametersEditor.parametersString.value.isEmpty()) {
                        if (patchedProjectOutput.defaultArguments.isNotEmpty()) {
                            programParametersEditor.parametersString.set(ParametersListUtil.join(patchedProjectOutput.defaultArguments))
                            programParametersEditor.defaultValue.set(ParametersListUtil.join(patchedProjectOutput.defaultArguments))
                        } else {
                            programParametersEditor.parametersString.set("")
                            programParametersEditor.defaultValue.set("")
                        }
                    }
                    workingDirectorySelector.defaultValue.set(patchedProjectOutput.workingDirectory)
                }
    }

    // TODO: FIX_WHEN. Add integration tests when enabled in the plugin.
    private fun recalculateTrackProjectOutput() {
        val selectedProject = projectSelector.project.valueOrNull ?: return
        val selectedTfm = tfmSelector.string.valueOrNull ?: return

        val programParameters = programParametersEditor.parametersString.value

        selectedProject.projectOutputs.singleOrNull { it.tfm == selectedTfm }?.let { projectOutput ->
            trackProjectExePath = exePathSelector.path.value == projectOutput.exePath

            val defaultArguments = projectOutput.defaultArguments
            trackProjectArguments = (defaultArguments.isEmpty() || ParametersListUtil.parse(programParameters).containsAll(defaultArguments))

            trackProjectWorkingDirectory = workingDirectorySelector.path.value == projectOutput.workingDirectory
        }

        val parametersPortMatch = portRegex.find(programParameters)
        val parametersPortValue = parametersPortMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: -1
        composeUrlString(parametersPortValue)
    }

    private fun composeUrlString(port: Int) {
        val currentUrl = urlEditor.text.value
        val originalUrl = if (currentUrl.isNotEmpty()) currentUrl else "http://localhost"

        val updatedUrl = URIBuilder(originalUrl).setPort(port).build().toString()

        urlEditor.text.set(updatedUrl)
        urlEditor.defaultValue.set(updatedUrl)
    }

    private fun handleProjectSelection(runnableProject: RunnableProject) {
        if (!isLoaded) return
        reloadTfmSelector(runnableProject)

        val startBrowserUrl = runnableProject.customAttributes.singleOrNull { it.key == Key.StartBrowserUrl }?.value ?: ""
        val launchBrowser = runnableProject.customAttributes.singleOrNull { it.key == Key.LaunchBrowser }?.value?.toBoolean() ?: false
        if (startBrowserUrl.isNotEmpty()) {
            urlEditor.defaultValue.set(startBrowserUrl)
            urlEditor.text.set(startBrowserUrl)
            dotNetBrowserSettingsEditor.settings.set(
                    BrowserSettings(
                            startAfterLaunch = launchBrowser,
                            withJavaScriptDebugger = dotNetBrowserSettingsEditor.settings.value.withJavaScriptDebugger,
                            myBrowser = dotNetBrowserSettingsEditor.settings.value.myBrowser))
        }

        environmentVariablesEditor.envs.set(runnableProject.environmentVariables.map { it.key to it.value }.toMap())
    }

    private fun reloadTfmSelector(runnableProject: RunnableProject) {
        tfmSelector.stringList.clear()
        runnableProject.projectOutputs.map { it.tfm }.sorted().forEach {
            tfmSelector.stringList.add(it)
        }
        if (tfmSelector.stringList.isNotEmpty()) {
            tfmSelector.string.set(tfmSelector.stringList.first())
        }
        handleChangeTfmSelection()
    }

    fun reset(projectFilePath: String,
              trackProjectExePath: Boolean,
              trackProjectArguments: Boolean,
              trackProjectWorkingDirectory: Boolean,
              projectTfm: String,
              exePath: String,
              programParameters: String,
              workingDirectory: String,
              functionNames: String,
              envs: Map<String, String>,
              passParentEnvs: Boolean,
              useExternalConsole: Boolean,
              isUnloadedProject: Boolean,
              dotNetStartBrowserParameters: DotNetStartBrowserParameters) {
        fun resetProperties(exePath: String, programParameters: String, workingDirectory: String) {
            super.reset(
                    exePath,
                    programParameters,
                    workingDirectory,
                    envs,
                    passParentEnvs,
                    false,
                    "",
                    useExternalConsole
            )
        }

        isLoaded = false

        this.trackProjectExePath = trackProjectExePath
        this.trackProjectArguments = trackProjectArguments
        this.trackProjectWorkingDirectory = trackProjectWorkingDirectory

        this.functionNamesEditor.defaultValue.value = ""
        this.functionNamesEditor.text.value = functionNames

        this.dotNetBrowserSettingsEditor.settings.set(BrowserSettings(
                startAfterLaunch = dotNetStartBrowserParameters.startAfterLaunch,
                withJavaScriptDebugger = dotNetStartBrowserParameters.withJavaScriptDebugger,
                myBrowser = dotNetStartBrowserParameters.browser))

        this.urlEditor.defaultValue.value = dotNetStartBrowserParameters.url
        this.urlEditor.text.value = dotNetStartBrowserParameters.url

        runnableProjectsModel.projects.adviseOnce(lifetime) { projectList ->

            if (projectFilePath.isEmpty() || projectList.none {
                        it.projectFilePath == projectFilePath && AzureFunctionsHostConfigurationType.isTypeApplicable(it.kind)
                    }) {
                // Case when project didn't selected otherwise we should generate fake project to avoid drop user settings.
                if (projectFilePath.isEmpty() || !isUnloadedProject) {
                    projectList.firstOrNull { type.isApplicable(it.kind) }?.let { project ->
                        projectSelector.project.set(project)
                        isLoaded = true
                        handleProjectSelection(project)
                    }
                } else {
                    val fakeProjectName = File(projectFilePath).name
                    val fakeProject = RunnableProject(
                            fakeProjectName, fakeProjectName, projectFilePath, RunnableProjectKind.Unloaded,
                            listOf(ProjectOutput(projectTfm, exePath, ParametersListUtil.parse(programParameters), workingDirectory, "")),
                            envs.map { EnvironmentVariable(it.key, it.value) }.toList(), null, listOf()
                    )
                    projectSelector.projectList.apply {
                        clear()
                        addAll(projectList + fakeProject)
                    }
                    projectSelector.project.set(fakeProject)
                    reloadTfmSelector(fakeProject)
                    resetProperties(exePath, programParameters, workingDirectory)
                }
            } else {
                projectList.singleOrNull {
                    it.projectFilePath == projectFilePath && AzureFunctionsHostConfigurationType.isTypeApplicable(it.kind)
                }?.let { project ->
                    projectSelector.project.set(project)

                    // Set TFM
                    reloadTfmSelector(project)
                    val projectTfmExists = project.projectOutputs.any { it.tfm == projectTfm }
                    val selectedTfm = if (projectTfmExists) projectTfm else project.projectOutputs.firstOrNull()?.tfm ?: ""
                    tfmSelector.string.set(selectedTfm)

                    // Set Project Output
                    val projectOutput = project.projectOutputs.singleOrNull { it.tfm == selectedTfm }
                    val effectiveExePath = if (trackProjectExePath && projectOutput != null) projectOutput.exePath else exePath
                    val effectiveProgramParameters =
                            if (trackProjectArguments && projectOutput != null && projectOutput.defaultArguments.isNotEmpty())
                                ParametersListUtil.join(projectOutput.defaultArguments).replace("\\\"", "\"")
                            else if (programParameters.isNotEmpty())
                                programParameters
                            else
                                // Handle the case when program parameters were set by changing TFM above and make sure it is not reset to empty.
                                programParametersEditor.defaultValue.value

                    programParametersEditor.defaultValue.set(effectiveProgramParameters)

                    val effectiveWorkingDirectory = if (trackProjectWorkingDirectory && projectOutput != null)
                        projectOutput.workingDirectory else workingDirectory

                    resetProperties(effectiveExePath, effectiveProgramParameters, effectiveWorkingDirectory)
                }
            }
            isLoaded = true
        }
    }
}
