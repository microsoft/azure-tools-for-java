package com.microsoft.intellij.runner.db

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.jetbrains.rider.projectView.nodes.ProjectModelNode
import com.jetbrains.rider.run.configurations.publishing.RiderContextPublishProvider
import com.microsoft.intellij.runner.db.dbconfig.RiderDatabaseConfiguration
import com.microsoft.intellij.runner.db.dbconfig.RiderDatabaseConfigurationType
import javax.swing.Icon

class AzureDatabaseContextPublishProvider : RiderContextPublishProvider {

    companion object {
        private const val RUN_CONFIG_NAME = "Create Azure SQL Database"
    }

    override val icon: Icon
        get() = IconLoader.getIcon("icons/Database.svg")

    override val name: String
        get() = "Create Azure SQL Database"

    override fun getConfigurationForNode(project: Project,
                                         projectModelNode: ProjectModelNode): Pair<RunConfiguration, ConfigurationFactory> {

        val configurationFactory = RiderDatabaseConfigurationType().configurationFactories.single()
        val configuration = RiderDatabaseConfiguration(project, configurationFactory, RUN_CONFIG_NAME)

        return Pair(configuration, configurationFactory)
    }

    override fun isAvailable(project: Project, projectModelNode: ProjectModelNode) = false
}
