package com.microsoft.intellij.runner.db.dbconfig

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class RiderDatabaseConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    companion object {
        private const val FACTORY_NAME = "Azure SQL Database"
    }

    override fun getName(): String {
        return FACTORY_NAME
    }

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return RiderDatabaseConfiguration(project, this, project.name)
    }

    override fun createConfiguration(name: String, template: RunConfiguration): RunConfiguration {
        return RiderDatabaseConfiguration(template.project, this, name)
    }
}