package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.microsoft.azure.hdinsight.spark.run.SparkSubmissionRunner

class CosmosServerlessSparkConfiguration(name: String, override val module: CosmosServerlessSparkConfigurationModule, cosmosServerlessSparkConfigurationFactory: CosmosServerlessSparkConfigurationFactory)
    : CosmosSparkRunConfiguration(name, module, cosmosServerlessSparkConfigurationFactory) {
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return LivySparkRunConfigurationSettingsEditor(CosmosServerlessSparkConfigurable(module.project))
    }

    override fun getSuggestedNamePrefix() : String {
        return "[Cosmos Serverless Spark]"
    }

    @Throws(RuntimeConfigurationException::class)
    override fun checkSubmissionConfigurationBeforeRun(runner: SparkSubmissionRunner) {
        super.checkSubmissionConfigurationBeforeRun(runner)

        val serverlessSubmitModel = module.model.submitModel as CosmosServerlessSparkSubmitModel

        if (serverlessSubmitModel.getSparkEventsDirectoryPath().isBlank()) {
            throw RuntimeConfigurationError("Can't save the configuration since spark events directory is not specified.")
        }
    }
}
