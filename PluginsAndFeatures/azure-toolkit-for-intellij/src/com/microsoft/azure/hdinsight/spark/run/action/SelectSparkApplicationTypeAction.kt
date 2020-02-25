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

import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Toggleable
import com.microsoft.azure.hdinsight.spark.run.configuration.*
import com.microsoft.azuretools.authmanage.CommonSettings
import com.microsoft.azuretools.ijidea.utility.AzureAnAction
import com.microsoft.azuretools.telemetry.TelemetryConstants
import com.microsoft.azuretools.telemetrywrapper.Operation
import com.microsoft.intellij.common.CommonConst
import com.microsoft.tooling.msservices.components.DefaultLoader


abstract class SelectSparkApplicationTypeAction
    : AzureAnAction() , Toggleable {
    override fun onActionPerformed(anActionEvent: AnActionEvent, operation: Operation?): Boolean {
        DefaultLoader.getIdeHelper().setApplicationProperty(CommonConst.SPARK_APPLICATION_TYPE, this.getSparkApplicationType().toString())
        return true
    }

    companion object {
        @JvmStatic
        fun getSelectedSparkApplicationType() : SparkApplicationType {
            if (!DefaultLoader.getIdeHelper().isApplicationPropertySet(CommonConst.SPARK_APPLICATION_TYPE)) return SparkApplicationType.None
            return SparkApplicationType.valueOf(DefaultLoader.getIdeHelper().getApplicationProperty(CommonConst.SPARK_APPLICATION_TYPE))
        }

        @JvmStatic
        fun getRunConfigurationType() : ConfigurationType? {
            return when(getSelectedSparkApplicationType()) {
                SparkApplicationType.None -> null
                SparkApplicationType.HDInsight -> LivySparkBatchJobRunConfigurationType.getInstance()
                SparkApplicationType.CosmosSpark -> CosmosSparkConfigurationType
                SparkApplicationType.CosmosServerlessSpark -> CosmosServerlessSparkConfigurationType
                SparkApplicationType.ArisSpark -> ArisSparkConfigurationType
                SparkApplicationType.ArcadiaSpark -> ArcadiaSparkConfigurationType
            }
        }
    }

    abstract fun getSparkApplicationType() : SparkApplicationType

    fun isSelected(): Boolean = getSparkApplicationType() == getSelectedSparkApplicationType()

    override fun update(e: AnActionEvent) {
        val selected = isSelected()
        val presentation = e.presentation
        presentation.putClientProperty(Toggleable.SELECTED_PROPERTY, selected)
        if (ActionPlaces.isPopupPlace(e.place)) {
            presentation.icon = null
        }
    }

    override fun getOperationName(event: AnActionEvent?): String {
        return TelemetryConstants.SELECT_DEFAULT_SPARK_APPLICATION_TYPE
    }

    override fun getServiceName(event: AnActionEvent?): String {
        return getSparkApplicationType().value
    }
}

class SelectNoneSparkTypeAction : SelectSparkApplicationTypeAction() {
    override fun getSparkApplicationType() : SparkApplicationType {
        return SparkApplicationType.None
    }
}

class SelectHDInsightSparkTypeAction : SelectSparkApplicationTypeAction() {
    override fun getSparkApplicationType() : SparkApplicationType {
        return SparkApplicationType.HDInsight
    }
}

class SelectCosmosSparkTypeAction : SelectSparkApplicationTypeAction() {
    override fun getSparkApplicationType() : SparkApplicationType {
        return SparkApplicationType.CosmosSpark
    }
}

class SelectCosmosServerlessSparkTypeAction : SelectSparkApplicationTypeAction() {
    override fun getSparkApplicationType() : SparkApplicationType {
        return SparkApplicationType.CosmosServerlessSpark
    }
}

class SelectArisSparkTypeAction : SelectSparkApplicationTypeAction() {
    override fun getSparkApplicationType() : SparkApplicationType {
        return SparkApplicationType.ArisSpark
    }
}

class SelectArcadiaSparkTypeAction : SelectSparkApplicationTypeAction() {
        override fun getSparkApplicationType() : SparkApplicationType {
            return SparkApplicationType.ArcadiaSpark
        }

    override fun update(e: AnActionEvent) {
        super.update(e)
        if (!CommonSettings.isProjectArcadiaFeatureEnabled) {
            e.presentation.isEnabledAndVisible = false
        }
    }
}

enum class SparkApplicationType(val value: String) {
    None("none"),
    HDInsight(TelemetryConstants.HDINSIGHT),
    CosmosSpark(TelemetryConstants.SPARK_ON_COSMOS),
    CosmosServerlessSpark(TelemetryConstants.SPARK_ON_COSMOS_SERVERLESS),
    ArisSpark(TelemetryConstants.SPARK_ON_SQL_SERVER),
    ArcadiaSpark(TelemetryConstants.SPARK_ON_ARCADIA)
}