package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil

class CosmosServerlessSparkConfigurationType : CosmosSparkConfigurationType() {
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
        fun getInstance(): ConfigurationType {
            return ConfigurationTypeUtil.findConfigurationType(CosmosSparkConfigurationType::class.java)
        }
    }
}
