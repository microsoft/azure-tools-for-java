package com.microsoft.intellij.runner.webapp.webappconfig

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class RiderWebAppConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    companion object {
        private const val FACTORY_NAME = "Azure Web App"
    }

    override fun getName(): String {
        return FACTORY_NAME
    }

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return RiderWebAppConfiguration(project, this, project.name)
    }

    override fun createConfiguration(name: String, template: RunConfiguration): RunConfiguration {
        return RiderWebAppConfiguration(template.project, this, name)
    }
}