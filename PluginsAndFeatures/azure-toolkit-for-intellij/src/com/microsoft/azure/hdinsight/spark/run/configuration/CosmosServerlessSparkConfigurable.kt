package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.spark.ui.CosmosServerlessSparkSubmissionPanelConfigurable
import com.microsoft.azure.hdinsight.spark.ui.SparkBatchJobConfigurable
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionContentPanelConfigurable

class CosmosServerlessSparkConfigurable(project: Project) : SparkBatchJobConfigurable(project) {
    override fun createSubmissionPanel(): SparkSubmissionContentPanelConfigurable =
            CosmosServerlessSparkSubmissionPanelConfigurable(project)
}