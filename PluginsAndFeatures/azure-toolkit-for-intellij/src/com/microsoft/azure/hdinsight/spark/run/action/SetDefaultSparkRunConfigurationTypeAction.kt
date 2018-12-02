package com.microsoft.azure.hdinsight.spark.run.action

import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.microsoft.azure.hdinsight.spark.run.configuration.CosmosSparkConfigurationType
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfigurationType


open class SetDefaultSparkRunConfigurationTypeAction(val sparkRunConfigurationType : SparkRunConfigurationType = SparkRunConfigurationType.None)
    : ToggleAction() {

    companion object {
        public var currentSparkRunConfigurationType : SparkRunConfigurationType = SparkRunConfigurationType.None

        fun getConfigurationType() : ConfigurationType = when(currentSparkRunConfigurationType) {
            SparkRunConfigurationType.None, SparkRunConfigurationType.HDInsight -> LivySparkBatchJobRunConfigurationType.getInstance()
            SparkRunConfigurationType.CosmosSpark, SparkRunConfigurationType.CosmosServerlessSpark -> CosmosSparkConfigurationType.getInstance()
        }
    }

    override fun isSelected(p0: AnActionEvent): Boolean {
        return sparkRunConfigurationType == currentSparkRunConfigurationType
    }

    override fun setSelected(p0: AnActionEvent, p1: Boolean) {
        currentSparkRunConfigurationType = sparkRunConfigurationType
    }

    enum class SparkRunConfigurationType {
        None,
        HDInsight,
        CosmosSpark,
        CosmosServerlessSpark
    }

    class None : SetDefaultSparkRunConfigurationTypeAction(SparkRunConfigurationType.None)
    class HDInsight : SetDefaultSparkRunConfigurationTypeAction(SparkRunConfigurationType.HDInsight)
    class CosmosSpark : SetDefaultSparkRunConfigurationTypeAction(SparkRunConfigurationType.CosmosSpark)
    class CosmosServerlessSpark : SetDefaultSparkRunConfigurationTypeAction(SparkRunConfigurationType.CosmosServerlessSpark)
}