/**
 * Copyright (c) 2019 JetBrains s.r.o.
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

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.getSelectedProject
import java.io.File

class AzureFunctionsConfigurationProducer
    : LazyRunConfigurationProducer<AzureFunctionsHostConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory =
            AzureFunctionsHostConfigurationFactory(AzureFunctionsHostConfigurationType())

    override fun setupConfigurationFromContext(configuration: AzureFunctionsHostConfiguration,
                                               context: ConfigurationContext, sourceElement: Ref<PsiElement>): Boolean {

        val selectedProject = context.getSelectedProject()
        val selectedElement = context.location?.psiElement
        if (selectedProject == null && selectedElement == null) return false

        val projects = context.project.solution.runnableProjectsModel.projects.valueOrNull ?: return false

        for (project in projects) {
            if (AzureFunctionsHostConfigurationType.isTypeApplicable(project.kind)) {
                val isProjectFile = selectedProject != null && project.projectFilePath == FileUtil.toSystemIndependentName(selectedProject.getFile()?.path ?: "")
                val isInProject = isInProject(project.projectFilePath, selectedElement?.containingFile?.virtualFile?.path)

                val functionName = AzureFunctionsRunMarkerContributor.tryResolveAzureFunctionName(selectedElement)

                if ((!isProjectFile && functionName.isNullOrBlank()) || !isInProject) continue

                // Create run configuration
                val prjToConfigure = AzureFunctionsRunnableProjectUtil.patchRunnableProjectOutputs(project)
                val projectOutput = prjToConfigure.projectOutputs.singleOrNull()

                if (functionName.isNullOrBlank()) {
                    configuration.name = prjToConfigure.fullName
                } else {
                    configuration.name = prjToConfigure.fullName + "." + functionName
                    configuration.parameters.functionNames = functionName
                }

                configuration.parameters.projectFilePath = prjToConfigure.projectFilePath
                configuration.parameters.projectKind = prjToConfigure.kind
                configuration.parameters.projectTfm = projectOutput?.tfm ?: ""
                configuration.parameters.exePath = projectOutput?.exePath ?: ""
                configuration.parameters.programParameters = ParametersListUtil.join(projectOutput?.defaultArguments
                        ?: listOf())
                configuration.parameters.workingDirectory = projectOutput?.workingDirectory ?: ""
                return true
            }
        }

        return false
    }

    override fun isPreferredConfiguration(self: ConfigurationFromContext?, other: ConfigurationFromContext?) = true

    override fun isConfigurationFromContext(configurationConfiguration: AzureFunctionsHostConfiguration,
                                            context: ConfigurationContext): Boolean {
        val item = context.getSelectedProject() ?: return false
        return FileUtil.toSystemIndependentName(item.getFile()?.path ?: "") ==
                configurationConfiguration.parameters.projectFilePath
    }

    private fun isInProject(projectFilePath: String, childPath: String?): Boolean {
        if (childPath == null) return false

        return File(childPath).startsWith(File(projectFilePath).parentFile)
    }
}