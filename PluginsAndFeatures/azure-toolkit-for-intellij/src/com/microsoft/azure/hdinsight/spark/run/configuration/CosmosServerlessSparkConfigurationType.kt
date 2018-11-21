package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.execution.configurations.ConfigurationFactory

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
}
