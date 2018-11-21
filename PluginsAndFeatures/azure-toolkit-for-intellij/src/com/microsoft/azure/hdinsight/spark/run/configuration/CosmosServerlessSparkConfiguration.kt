package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.options.SettingsEditor

class CosmosServerlessSparkConfiguration(name: String, override val module: CosmosServerlessSparkConfigurationModule, cosmosServerlessSparkConfigurationFactory: CosmosServerlessSparkConfigurationFactory)
    : CosmosSparkRunConfiguration(name, module, cosmosServerlessSparkConfigurationFactory) {
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return LivySparkRunConfigurationSettingsEditor(CosmosServerlessSparkConfigurable(module.project))
    }

    override fun getSuggestedNamePrefix() : String {
        return "[Cosmos Serverless Spark]"
    }
}
