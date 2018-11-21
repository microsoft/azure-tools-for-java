package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.CreateSparkBatchJobParameters
import com.microsoft.azure.hdinsight.spark.common.CosmosSparkSubmitModel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter
import com.microsoft.azuretools.utils.Pair
import java.util.stream.Stream

class CosmosServerlessSparkSubmitModel(project: Project) : CosmosSparkSubmitModel(project) {
    private var submissionParameter : CreateSparkBatchJobParameters = CreateSparkBatchJobParameters()
            .withSparkEventsDirectoryPath("sp1ark-events")

    @Attribute("sparkevents_directory")
    fun getSparkEventsDirectoryPath(): String {
        return submissionParameter.sparkEventsDirectoryPath()
    }

    @Attribute("sparkevents_directory")
    fun setSparkEventsDirectoryPath(path: String) {
        submissionParameter.withSparkEventsDirectoryPath(path)
    }

    override fun getDefaultParameters(): Stream<Pair<String, out Any>> {
        return listOf(
                Pair(SparkSubmissionParameter.DriverMemory, SparkSubmissionParameter.DriverMemoryDefaultValue),
                Pair(SparkSubmissionParameter.DriverCores, SparkSubmissionParameter.DriverCoresDefaultValue),
                Pair(SparkSubmissionParameter.ExecutorMemory, SparkSubmissionParameter.ExecutorMemoryDefaultValue),
                Pair(SparkSubmissionParameter.ExecutorCores, SparkSubmissionParameter.ExecutorCoresDefaultValue),
                Pair(SparkSubmissionParameter.NumExecutors, SparkSubmissionParameter.NumExecutorsDefaultValue)
        ).stream()
    }

    override fun getSubmissionParameter(): SparkSubmissionParameter {
        return submissionParameter
    }
}