package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.openapi.command.impl.DummyProject
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Transient
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.CreateSparkBatchJobParameters
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel
import com.microsoft.azuretools.utils.Pair
import java.util.stream.Stream

class CosmosServerlessSparkSubmitModel(project: Project) : SparkSubmitModel(project,
    CreateSparkBatchJobParameters().withSparkEventsDirectoryPath("spark-events")) {
    constructor(): this(DummyProject.getInstance())

    @get:Transient @set:Transient var sparkEventsDirectoryPrefix: String = "adl://*.azuredatalakestore.net/"

    @Attribute("sparkevents_directory")
    fun getSparkEventsDirectoryPath(): String {
        return (submissionParameter as CreateSparkBatchJobParameters).sparkEventsDirectoryPath()
    }

    @Attribute("sparkevents_directory")
    fun setSparkEventsDirectoryPath(path: String) {
        (submissionParameter as CreateSparkBatchJobParameters).withSparkEventsDirectoryPath(path)
    }

    override fun getDefaultParameters(): Stream<Pair<String, out Any>> {
        return listOf(
                Pair(CreateSparkBatchJobParameters.DriverMemory, CreateSparkBatchJobParameters.DriverMemoryDefaultValue),
                Pair(CreateSparkBatchJobParameters.DriverCores, CreateSparkBatchJobParameters.DriverCoresDefaultValue),
                Pair(CreateSparkBatchJobParameters.ExecutorMemory, CreateSparkBatchJobParameters.ExecutorMemoryDefaultValue),
                Pair(CreateSparkBatchJobParameters.ExecutorCores, CreateSparkBatchJobParameters.ExecutorCoresDefaultValue),
                Pair(CreateSparkBatchJobParameters.NumExecutors, CreateSparkBatchJobParameters.NumExecutorsDefaultValue)
        ).stream()
    }

    override fun getSubmissionParameter(): SparkSubmissionParameter {
        return submissionParameter
    }

    override fun getSparkClusterTypeDisplayName(): String = "Cosmos Serverless Spark account"
}