package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.spark.ui.CosmosServerlessSparkSubmissionPanel
import com.microsoft.azure.hdinsight.spark.ui.SparkBatchJobConfigurable
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionContentPanel
import com.microsoft.azuretools.telemetry.TelemetryConstants

class CosmosServerlessSparkConfigurable(project: Project) : SparkBatchJobConfigurable(project) {
    override fun createSubmissionPanel(): SparkSubmissionContentPanel =
            CosmosServerlessSparkSubmissionPanel(project)

    override fun getServiceName(): String {
        return TelemetryConstants.SPARK_ON_COSMOS_SERVERLESS
    }
}