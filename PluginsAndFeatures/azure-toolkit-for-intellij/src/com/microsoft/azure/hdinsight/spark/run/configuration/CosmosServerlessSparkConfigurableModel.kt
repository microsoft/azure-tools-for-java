package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.spark.common.CosmosSparkConfigurableModel
import com.microsoft.azure.hdinsight.spark.common.SparkLocalRunConfigurableModel

class CosmosServerlessSparkConfigurableModel(project: Project) : CosmosSparkConfigurableModel(project) {
    private val cosmosServerlessSparkSubmitModel = CosmosServerlessSparkSubmitModel(project)

    init {
        localRunConfigurableModel = SparkLocalRunConfigurableModel(project)
        submitModel = cosmosServerlessSparkSubmitModel
    }
}
