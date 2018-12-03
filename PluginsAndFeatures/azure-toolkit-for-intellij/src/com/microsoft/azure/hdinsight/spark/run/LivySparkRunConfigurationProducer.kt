package com.microsoft.azure.hdinsight.spark.run

import com.microsoft.azure.hdinsight.spark.run.action.DefaultSparkApplicationTypeAction
import com.microsoft.azure.hdinsight.spark.run.action.SparkApplicationType
import com.microsoft.azure.hdinsight.spark.run.configuration.CosmosSparkConfigurationType
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfigurationType

class LivySparkRunConfigurationProducer : SparkBatchJobLocalRunConfigurationProducer(
        LivySparkBatchJobRunConfigurationType.getInstance(),
        SparkApplicationType.HDInsight
)
