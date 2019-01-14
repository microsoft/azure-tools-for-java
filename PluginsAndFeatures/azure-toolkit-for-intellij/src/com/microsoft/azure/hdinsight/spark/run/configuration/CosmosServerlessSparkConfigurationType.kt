package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.microsoft.azure.hdinsight.common.CommonConst
import com.microsoft.azuretools.authmanage.CommonSettings
import com.microsoft.intellij.util.PluginUtil
import javax.swing.Icon

object CosmosServerlessSparkConfigurationType : ConfigurationType {
    override fun getIcon(): Icon {
        // TODO: should use Cosmos Serverless icon
        return PluginUtil.getIcon("/icons/${CommonConst.AZURE_SERVERLESS_SPARK_ROOT_ICON_PATH}")
    }

    override fun getConfigurationTypeDescription(): String {
        return "Spark on Cosmos Serverless Configuration"
    }

    override fun getDisplayName(): String {
        return "Apache Spark on Cosmos Serverless"
    }

    override fun getId(): String {
        return "CosmosServerlessSparkConfiguration"
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return when {
            CommonSettings.isCosmosServerlessEnabled -> arrayOf(CosmosServerlessSparkConfigurationFactory(this))
            else -> arrayOf()
        }
    }
}
