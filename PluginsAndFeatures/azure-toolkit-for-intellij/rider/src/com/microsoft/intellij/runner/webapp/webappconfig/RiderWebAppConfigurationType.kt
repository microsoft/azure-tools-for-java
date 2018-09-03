package com.microsoft.intellij.runner.webapp.webappconfig

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import icons.RiderIcons
import org.jetbrains.annotations.Nls
import javax.swing.Icon

class RiderWebAppConfigurationType : ConfigurationType {

    companion object {

        private const val RUN_CONFIG_TYPE_ID = "RiderAzurePublish"
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

    override fun getIcon(): Icon {
        // TODO: SD -- This should be replaced with a custom icon
        return RiderIcons.Publish.PublishAzure
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(RiderWebAppConfigurationFactory(this))
    }
}
