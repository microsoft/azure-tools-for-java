package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.microsoft.azure.hdinsight.common.CommonConst
import com.microsoft.intellij.util.PluginUtil
import javax.swing.Icon

class CosmosServerlessSparkConfigurationType : ConfigurationType {
    companion object {
        @JvmStatic
        val instance by lazy { ConfigurationTypeUtil.findConfigurationType(CosmosServerlessSparkConfigurationType::class.java) }
    }

    override fun getIcon(): Icon {
        // TODO: should use Cosmos Serverless icon
        return PluginUtil.getIcon("/icons/${CommonConst.AZURE_SERVERLESS_SPARK_ROOT_ICON_PATH}")
    }

    override fun getConfigurationTypeDescription(): String {
        return "Cosmos Serverless Spark Job Configuration"
    }

    override fun getDisplayName(): String {
        return "Cosmos Serverless Spark"
    }

    override fun getId(): String {
        return "CosmosServerlessSparkConfiguration"
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(CosmosServerlessSparkConfigurationFactory(this))
    }

    companion object {
        @JvmStatic
        fun getInstance(): CosmosServerlessSparkConfigurationType {
            return ConfigurationTypeUtil.findConfigurationType(CosmosServerlessSparkConfigurationType::class.java)
        }
    }
}
