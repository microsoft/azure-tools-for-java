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

import com.intellij.execution.configurations.RuntimeConfigurationWarning
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel
import com.microsoft.azure.hdinsight.spark.run.action.SparkApplicationType
import com.microsoft.azure.hdinsight.spark.ui.ArcadiaSparkClusterListRefreshableCombo
import com.microsoft.azure.hdinsight.spark.ui.SparkClusterListRefreshableCombo
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionContentPanel
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkCompute
import com.microsoft.azure.synapsesoc.common.SynapseCosmosSparkPool

class ArcadiaSparkSubmissionContentPanel (project: Project) : SparkSubmissionContentPanel(project, "Synapse Spark") {
    override val clustersSelection: SparkClusterListRefreshableCombo by lazy { ArcadiaSparkClusterListRefreshableCombo().apply {
        Disposer.register(this@ArcadiaSparkSubmissionContentPanel, this@apply)
    } }

    override val clusterHint: String
        get() = "Apache Spark Pool for Azure Synapse"

    override fun getData(data: SparkSubmitModel) {
        // Component -> Data
        super.getData(data)

        val arcadiaData = data as? ArcadiaSparkSubmitModel ?: return
        val cluster = viewModel.clusterSelection.let {
            it.findClusterById(it.clusterListModelBehavior.value, it.toSelectClusterByIdBehavior.value)
        }

        if (cluster != null) {
            arcadiaData.tenantId = cluster.subscription.tenantId
            arcadiaData.sparkWorkspace = (cluster as? ArcadiaSparkCompute)?.workSpace?.name
            arcadiaData.sparkCompute = cluster.name
            arcadiaData.livyUri = cluster.connectionUrl
                    ?: throw RuntimeConfigurationWarning("Can't get Apache Spark Pool for Azure Synapse connection URL")
            arcadiaData.sparkApplicationType = when (cluster) {
                is SynapseCosmosSparkPool -> SparkApplicationType.CosmosSpark
                is ArcadiaSparkCompute -> SparkApplicationType.ArcadiaSpark
                else -> SparkApplicationType.None
            }
        }
    }
}