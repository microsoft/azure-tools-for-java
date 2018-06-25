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

package com.microsoft.azure.hdinsight.spark.ui

import com.google.common.collect.ImmutableList
import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.common.CallBack
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessClusterManager
import com.microsoft.azure.hdinsight.spark.common.ServerlessSparkSubmitModel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel
import rx.schedulers.Schedulers

class ServerlessSparkSubmissionPanelConfigurable(private val project: Project, callBack: CallBack?, submissionPanel: SparkSubmissionContentPanel)
    : SparkSubmissionContentPanelConfigurable(project, callBack, submissionPanel), ILogger {
    override fun refreshClusterListAsync() {
        submissionPanel.setClustersListRefreshEnabled(false)

        AzureSparkServerlessClusterManager.getInstance()
                .fetchClusters()
                .subscribeOn(Schedulers.io())
                .map { clusterManager -> clusterManager.clusters }
                .doOnEach { submissionPanel.setClustersListRefreshEnabled(true) }
                .subscribe(
                        { clusters ->
                            refreshClusterSelection(clusters.asList())
                            if (submissionPanel.clustersListComboBox.comboBox.selectedItem != null) {
                                submissionPanel.clusterSelectedSubject.onNext(
                                        submissionPanel.clustersListComboBox.comboBox.selectedItem as String)
                            }
                        },
                        { err -> log().warn("Project ${project.name} failed to refresh Azure Data Lake Spark Pool", err) }
                )
    }

    override fun getClusterDetails(): ImmutableList<IClusterDetail> {
        return ImmutableList.copyOf(
                AzureSparkServerlessClusterManager.getInstance().clusters.asList().filterIsInstance<AzureSparkServerlessCluster>())
    }

    override fun resetClusterDetailsToComboBoxModel(destSubmitModel: SparkSubmitModel, cachedClusterDetails: MutableList<IClusterDetail>) {
        // Reset submit model
        destSubmitModel.setCachedClusterDetailsWithTitleMapping(cachedClusterDetails)

        // Reset cluster combo box model
        destSubmitModel.clusterComboBoxModel.removeAllElements()
        cachedClusterDetails.forEach { destSubmitModel.clusterComboBoxModel.addElement(it.title) }
    }

    override fun getData(data: SparkSubmitModel?) {
        // Component -> Data
        val serverlessData = data as ServerlessSparkSubmitModel
        val cluster = submitModel.selectedClusterDetail
                        .map { it as AzureSparkServerlessCluster }
                        .orElse(null)

        if (cluster != null) {
            serverlessData.tenantId = cluster.subscription.tenantId
            serverlessData.accountName = cluster.account.name
            serverlessData.clusterId = cluster.guid
            serverlessData.livyUri = cluster.livyUri
        }

        super.getData(data)
    }

}