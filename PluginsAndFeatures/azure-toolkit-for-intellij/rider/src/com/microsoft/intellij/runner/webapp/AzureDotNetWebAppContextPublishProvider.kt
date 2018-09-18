package com.microsoft.intellij.runner.webapp

import com.intellij.execution.configurations.ConfigurationFactory
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
        private const val RUN_CONFIG_NAME = "Publish %s to Azure"
    }

    override val icon: Icon
        get() = IconLoader.getIcon("icons/WebApp.svg")

    override val name: String
        get() = "Publish to Azure"

    override fun getConfigurationForNode(project: Project,
                                         projectModelNode: ProjectModelNode): Pair<RunConfiguration, ConfigurationFactory> {

        val projectData = RiderContextPublishProvider.getProjectDataRecursive(project, projectModelNode)
                ?: error("Unexpected project node type. Cannot get project data for node ${projectModelNode.location}")

        val configurationFactory = RiderWebAppConfigurationType().configurationFactories.single()
        val configuration = RiderWebAppConfiguration(project, configurationFactory, String.format(RUN_CONFIG_NAME, projectData.value.projectName))

        configuration.model.webAppModel.publishableProject = projectData.value

        return Pair(configuration, configurationFactory)
    }

    override fun isAvailable(project: Project, projectModelNode: ProjectModelNode): Boolean {
        val projectData = RiderContextPublishProvider.getProjectData(project, projectModelNode)
        return projectData != null && projectData.value.isWeb && (projectData.value.isDotNetCore || SystemInfo.isWindows)
    }
}
