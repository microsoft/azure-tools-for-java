package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import javax.swing.Icon

object CosmosServerlessSparkConfigurationType : ConfigurationType {
    override fun getIcon(): Icon {
        // TODO: should use Cosmos Serverless icon
        return IntelliJAzureIcons.getIcon(AzureIcons.ApacheSparkOnCosmos.MODULE)
    }

    override fun getConfigurationTypeDescription(): String {
        return "Apache Spark on Cosmos Serverless Configuration"
    }

    override fun getDisplayName(): String {
        return "Apache Spark on Cosmos Serverless"
    }

    override fun getId(): String {
        return "CosmosServerlessSparkConfiguration"
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(CosmosServerlessSparkConfigurationFactory(this))
    }
}
