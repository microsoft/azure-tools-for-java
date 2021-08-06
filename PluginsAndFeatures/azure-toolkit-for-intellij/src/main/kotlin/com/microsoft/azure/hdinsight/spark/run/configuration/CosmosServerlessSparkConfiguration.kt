package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.spark.run.SparkSubmissionRunner
import com.microsoft.azure.hdinsight.spark.run.action.SparkApplicationType

class CosmosServerlessSparkConfiguration(
        project: Project,
        name: String,
        override val model: CosmosServerlessSparkConfigurableModel,
        cosmosServerlessSparkConfigurationFactory: CosmosServerlessSparkConfigurationFactory)
    : CosmosSparkRunConfiguration(project, name, model, cosmosServerlessSparkConfigurationFactory) {
    override fun getSparkApplicationType(): SparkApplicationType {
        return SparkApplicationType.CosmosServerlessSpark
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return LivySparkRunConfigurationSettingsEditor(CosmosServerlessSparkConfigurable(project))
    }

    override fun getSuggestedNamePrefix() : String = "[Apache Spark on Cosmos Serverless]"

    override fun getErrorMessageClusterNull(): String = "The account should be selected as the target for Spark application submission"

    @Throws(RuntimeConfigurationException::class)
    override fun checkSubmissionConfigurationBeforeRun(runner: SparkSubmissionRunner) {
        super.checkSubmissionConfigurationBeforeRun(runner)

        val serverlessSubmitModel = model.submitModel as CosmosServerlessSparkSubmitModel

        if (serverlessSubmitModel.getSparkEventsDirectoryPath().isBlank()) {
            throw RuntimeConfigurationError("Can't save the configuration since spark events directory is not specified.")
        }
    }
}
