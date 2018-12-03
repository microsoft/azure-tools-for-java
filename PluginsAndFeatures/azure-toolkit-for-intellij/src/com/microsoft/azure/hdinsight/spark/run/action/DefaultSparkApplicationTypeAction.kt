package com.microsoft.azure.hdinsight.spark.run.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.microsoft.intellij.common.CommonConst
import com.microsoft.tooling.msservices.components.DefaultLoader


open class DefaultSparkApplicationTypeAction(private val sparkApplicationType : SparkApplicationType = SparkApplicationType.HDInsight)
    : ToggleAction() {
    companion object {
        @JvmStatic
        fun getSelectedSparkApplicationType() : SparkApplicationType {
            var isExist = DefaultLoader.getIdeHelper().isApplicationPropertySet(CommonConst.SPARK_APPLICATION_TYPE)
            if(!isExist) {
                return SparkApplicationType.HDInsight
            }
            return SparkApplicationType.valueOf(DefaultLoader.getIdeHelper().getApplicationProperty(CommonConst.SPARK_APPLICATION_TYPE))
        }
    }

    override fun isSelected(p0: AnActionEvent): Boolean = this.sparkApplicationType == getSelectedSparkApplicationType()

    override fun setSelected(p0: AnActionEvent, p1: Boolean) =
            DefaultLoader.getIdeHelper().setApplicationProperty(CommonConst.SPARK_APPLICATION_TYPE, this.sparkApplicationType.toString())

    class HDInsight : DefaultSparkApplicationTypeAction(SparkApplicationType.HDInsight)
    class CosmosSpark : DefaultSparkApplicationTypeAction(SparkApplicationType.CosmosSpark)
    class CosmosServerlessSpark : DefaultSparkApplicationTypeAction(SparkApplicationType.CosmosServerlessSpark)
}

enum class SparkApplicationType {
    HDInsight,
    CosmosSpark,
    CosmosServerlessSpark
}