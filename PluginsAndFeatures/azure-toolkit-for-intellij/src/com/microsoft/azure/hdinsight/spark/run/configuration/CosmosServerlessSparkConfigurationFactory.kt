package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

open class CosmosServerlessSparkConfigurationFactory (type: ConfigurationType) :
        ConfigurationFactory(type) {
    companion object {
        @JvmStatic
        val NAME = "Cosmos Serverless Spark"
    }

    override fun createTemplateConfiguration(project: Project): RunConfiguration = CosmosServerlessSparkConfiguration(
            project,
            NAME,
            CosmosServerlessSparkConfigurableModel(project),
            this
    )

    override fun getName(): String {
        return NAME
    }

    override fun getId(): String = NAME
}