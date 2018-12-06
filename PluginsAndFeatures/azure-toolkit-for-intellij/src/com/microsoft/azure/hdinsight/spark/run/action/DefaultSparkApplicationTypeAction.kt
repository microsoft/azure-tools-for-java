package com.microsoft.azure.hdinsight.spark.run.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Toggleable
import com.microsoft.azuretools.azurecommons.helpers.NotNull
import com.microsoft.azuretools.ijidea.utility.AzureAnAction
import com.microsoft.intellij.common.CommonConst
import com.microsoft.tooling.msservices.components.DefaultLoader


open class DefaultSparkApplicationTypeAction(private val sparkApplicationType : SparkApplicationType = SparkApplicationType.HDInsight)
    : AzureAnAction() , Toggleable {
    override fun onActionPerformed(e: AnActionEvent?) {
        DefaultLoader.getIdeHelper().setApplicationProperty(CommonConst.SPARK_APPLICATION_TYPE, this.sparkApplicationType.toString())
        val presentation = e!!.presentation
        presentation.putClientProperty(Toggleable.SELECTED_PROPERTY, true)
    }

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

    fun isSelected(): Boolean = this.sparkApplicationType == getSelectedSparkApplicationType()

    override fun update(@NotNull e: AnActionEvent) {
        val selected = isSelected()
        val presentation = e.presentation
        presentation.putClientProperty(Toggleable.SELECTED_PROPERTY, selected)
        if (e.isFromContextMenu) {
            //force to show check marks instead of toggled icons in context menu
            presentation.icon = null
        }
    }

    class HDInsight : DefaultSparkApplicationTypeAction(SparkApplicationType.HDInsight)
    class CosmosSpark : DefaultSparkApplicationTypeAction(SparkApplicationType.CosmosSpark)
    class CosmosServerlessSpark : DefaultSparkApplicationTypeAction(SparkApplicationType.CosmosServerlessSpark)
}

enum class SparkApplicationType {
    HDInsight,
    CosmosSpark,
    CosmosServerlessSpark
}