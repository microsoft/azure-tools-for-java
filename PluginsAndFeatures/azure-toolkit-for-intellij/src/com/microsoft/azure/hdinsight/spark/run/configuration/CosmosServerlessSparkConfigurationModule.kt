package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.openapi.project.Project

class CosmosServerlessSparkConfigurationModule(project: Project) : CosmosSparkConfigurationModule(project) {
    override val model = CosmosServerlessSparkConfigurableModel(project)
}
