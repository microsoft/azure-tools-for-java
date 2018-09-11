package com.microsoft.intellij.runner.webapp.webappconfig

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.util.IconLoader
import com.microsoft.tooling.msservices.components.DefaultLoader
import org.jetbrains.annotations.Nls
import javax.swing.Icon

class RiderWebAppConfigurationType : ConfigurationType {

    companion object {
        private const val RUN_CONFIG_TYPE_ID = "AzureDotNetWebAppPublish"
        private const val RUN_CONFIG_TYPE_NAME = "Azure Publish to Web App"
        private const val RUN_CONFIG_TYPE_DESCRIPTION = "Azure Publish to Web App configuration"
    }

    override fun getId(): String {
        return RUN_CONFIG_TYPE_ID
    }

    @Nls
    override fun getDisplayName(): String {
        return RUN_CONFIG_TYPE_NAME
    }

    @Nls
    override fun getConfigurationTypeDescription(): String {
        return RUN_CONFIG_TYPE_DESCRIPTION
    }

    override fun getIcon(): Icon = IconLoader.getIcon("icons/publishAzure.svg")

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(RiderWebAppConfigurationFactory(this))
    }
}
