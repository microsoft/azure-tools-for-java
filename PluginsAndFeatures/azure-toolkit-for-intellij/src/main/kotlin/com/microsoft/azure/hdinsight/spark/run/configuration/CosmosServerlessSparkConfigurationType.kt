package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.microsoft.intellij.helpers.AzureIconLoader
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol
import javax.swing.Icon

object CosmosServerlessSparkConfigurationType : ConfigurationType {
    override fun getIcon(): Icon {
        // TODO: should use Cosmos Serverless icon
        return AzureIconLoader.loadIcon(AzureIconSymbol.ApacheSparkOnCosmos.MODULE)
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
