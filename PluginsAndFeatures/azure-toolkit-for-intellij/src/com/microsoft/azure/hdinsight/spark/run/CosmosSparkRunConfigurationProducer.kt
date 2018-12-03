package com.microsoft.azure.hdinsight.spark.run

import com.microsoft.azure.hdinsight.spark.run.action.SparkApplicationType
import com.microsoft.azure.hdinsight.spark.run.configuration.CosmosSparkConfigurationType

class CosmosSparkRunConfigurationProducer : SparkBatchJobLocalRunConfigurationProducer(
        CosmosSparkConfigurationType.getInstance(),
        SparkApplicationType.CosmosSpark
)

