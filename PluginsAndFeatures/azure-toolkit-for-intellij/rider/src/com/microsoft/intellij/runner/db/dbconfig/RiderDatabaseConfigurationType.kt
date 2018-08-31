package com.microsoft.intellij.runner.db.dbconfig

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import icons.RiderIcons
import org.jetbrains.annotations.Nls
import javax.swing.Icon

class RiderDatabaseConfigurationType : ConfigurationType {

    companion object {

        private const val RUN_CONFIG_TYPE_ID = "RiderAzureCreateSqlDatabase"
        private const val RUN_CONFIG_TYPE_NAME = "Azure Create SQL Database"
        private const val RUN_CONFIG_TYPE_DESCRIPTION = "Azure create SQL Database configuration"

        val instance: RiderDatabaseConfigurationType
            get() = ConfigurationTypeUtil.findConfigurationType(RiderDatabaseConfigurationType::class.java)
    }

    val databaseConfigurationFactory: RiderDatabaseConfigurationFactory
        get() = RiderDatabaseConfigurationFactory(this)

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
        return arrayOf(databaseConfigurationFactory)
    }
}
