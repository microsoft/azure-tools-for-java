/**
 * Copyright (c) 2018 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.webapp

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.SystemInfo
import com.jetbrains.rider.projectView.nodes.ProjectModelNode
import com.jetbrains.rider.run.configurations.publishing.RiderContextPublishProvider
import com.microsoft.intellij.runner.webapp.webappconfig.RiderWebAppConfiguration
import com.microsoft.intellij.runner.webapp.webappconfig.RiderWebAppConfigurationType
import javax.swing.Icon

class AzureDotNetWebAppContextPublishProvider : RiderContextPublishProvider {

    companion object {
        private const val RUN_CONFIG_PROJECT_NAME = "Publish %s to Azure"
        private const val RUN_CONFIG_NAME = "Publish to Azure"
    }

    override val icon: Icon
        get() = IconLoader.getIcon("icons/WebApp.svg")

    override val name: String
        get() = RUN_CONFIG_NAME

    override fun getConfigurationForNode(project: Project,
                                         projectModelNode: ProjectModelNode): Pair<RunConfiguration, ConfigurationFactory> {

        val projectData = RiderContextPublishProvider.getProjectDataRecursive(project, projectModelNode)
                ?: error("Unexpected project node type. Cannot get project data for node ${projectModelNode.location}")

        val factory = ConfigurationTypeUtil.findConfigurationType(RiderWebAppConfigurationType::class.java).configurationFactories.single()
        val configuration = RiderWebAppConfiguration(project, factory, String.format(RUN_CONFIG_PROJECT_NAME, projectData.value.projectName))

        configuration.model.webAppModel.publishableProject = projectData.value

        return Pair(configuration, factory)
    }

    override fun isAvailable(project: Project, projectModelNode: ProjectModelNode): Boolean {
        val projectData = RiderContextPublishProvider.getProjectData(project, projectModelNode)
        return projectData != null && projectData.value.isWeb && (projectData.value.isDotNetCore || SystemInfo.isWindows)
    }
}
