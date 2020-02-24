/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.microsoft.azure.hdinsight.spark.run.action.SparkApplicationType
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkCompute
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkComputeManager
import com.microsoft.azure.synapsesoc.common.SynapseCosmosSparkPool

class ArcadiaSparkConfiguration (name: String, val module: ArcadiaSparkConfigurationModule, factory: ArcadiaSparkConfigurationFactory) : LivySparkBatchJobRunConfiguration(module.model, factory, module, name) {
    override fun getSparkApplicationType(): SparkApplicationType {
        val cluster = try {
            val arcadiaModel = module.model.submitModel as ArcadiaSparkSubmitModel
            ArcadiaSparkComputeManager.getInstance()
                    .findCompute(arcadiaModel.tenantId, arcadiaModel.sparkWorkspace, arcadiaModel.sparkCompute)
                    .toBlocking()
                    .first()
        } catch (ignore: Exception) {
        }

        return when (cluster) {
            is SynapseCosmosSparkPool -> SparkApplicationType.CosmosSpark
            is ArcadiaSparkCompute -> SparkApplicationType.ArcadiaSpark
            else -> SparkApplicationType.None
        }
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return LivySparkRunConfigurationSettingsEditor(ArcadiaSparkConfigurable(module.project))
    }

    override fun getSuggestedNamePrefix(): String {
        return "[Spark on Synapse]"
    }

    override fun getErrorMessageClusterNull(): String {
        return "Spark pool should be selected as the target for Spark application submission"
    }
}