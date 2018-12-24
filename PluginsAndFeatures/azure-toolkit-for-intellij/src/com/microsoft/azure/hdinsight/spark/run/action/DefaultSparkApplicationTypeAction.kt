/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.run.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Toggleable
import com.microsoft.azuretools.azurecommons.helpers.NotNull
import com.microsoft.azuretools.ijidea.utility.AzureAnAction
import com.microsoft.intellij.common.CommonConst
import com.microsoft.tooling.msservices.components.DefaultLoader


open class DefaultSparkApplicationTypeAction(private val sparkApplicationType : SparkApplicationType = SparkApplicationType.HDInsight)
    : AzureAnAction() , Toggleable {
    override fun onActionPerformed(e: AnActionEvent) {
        DefaultLoader.getIdeHelper().setApplicationProperty(CommonConst.SPARK_APPLICATION_TYPE, this.sparkApplicationType.toString())
        val presentation = e.presentation
        presentation.putClientProperty(Toggleable.SELECTED_PROPERTY, true)
    }

    companion object {
        @JvmStatic
        fun getSelectedSparkApplicationType() : SparkApplicationType {
            if (!DefaultLoader.getIdeHelper().isApplicationPropertySet(CommonConst.SPARK_APPLICATION_TYPE)) return SparkApplicationType.None
            return SparkApplicationType.valueOf(DefaultLoader.getIdeHelper().getApplicationProperty(CommonConst.SPARK_APPLICATION_TYPE))
        }
    }

    fun isSelected(): Boolean = this.sparkApplicationType == getSelectedSparkApplicationType()

    override fun update(e: AnActionEvent) {
        val selected = isSelected()
        val presentation = e.presentation
        presentation.putClientProperty(Toggleable.SELECTED_PROPERTY, selected)
        if (e.isFromContextMenu) {
            //force to show check marks instead of toggled icons in context menu
            presentation.icon = null
        }
    }

    class None : DefaultSparkApplicationTypeAction(SparkApplicationType.None)
    class HDInsight : DefaultSparkApplicationTypeAction(SparkApplicationType.HDInsight)
    class CosmosSpark : DefaultSparkApplicationTypeAction(SparkApplicationType.CosmosSpark)
    class CosmosServerlessSpark : DefaultSparkApplicationTypeAction(SparkApplicationType.CosmosServerlessSpark)
}

enum class SparkApplicationType {
    None,
    HDInsight,
    CosmosSpark,
    CosmosServerlessSpark
}