package com.microsoft.azure.hdinsight.spark.run

import com.microsoft.azure.hdinsight.spark.run.action.SparkApplicationType
import com.microsoft.azure.hdinsight.spark.run.configuration.ArisSparkConfigurationType

class ArisSparkRunConfigurationProducer : SparkBatchJobLocalRunConfigurationProducer(
        ArisSparkConfigurationType,
        SparkApplicationType.ArisSpark
)