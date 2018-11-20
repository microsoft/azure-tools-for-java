package com.microsoft.azure.hdinsight.spark.ui

import com.google.common.collect.ImmutableSortedSet
import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessClusterManager
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel
import com.microsoft.azure.hdinsight.spark.run.configuration.CosmosServerlessSparkSubmitModel
import com.microsoft.azuretools.azurecommons.helpers.NotNull
import rx.Observable
import javax.swing.JComponent

open class CosmosServerlessSparkSubmissionContentPanelConfigurable : SparkSubmissionContentPanelConfigurable {
    private val cosmosServerlessSparkSubmissionContentPanel = CosmosServerlessSparkSubmissionContentPanel()

    constructor(project: Project) : super(project, true) {
        super.submissionPanel = cosmosServerlessSparkSubmissionContentPanel
        super.registerCtrlListeners()
    }

    @NotNull
    override fun getClusterDetails(): ImmutableSortedSet<out IClusterDetail> {
        return ImmutableSortedSet.copyOf({ x, y -> x.title.compareTo(y.title, ignoreCase = true) },
                AzureSparkServerlessClusterManager.getInstance().accounts)
    }

    @NotNull
    override fun getClusterDetailsWithRefresh(): Observable<ImmutableSortedSet<out IClusterDetail>> {
        return Observable.fromCallable { AzureSparkServerlessClusterManager.getInstance().accounts }
                .map { list -> ImmutableSortedSet.copyOf({ x, y -> x.title.compareTo(y.title, ignoreCase = true) }, list) }
    }

    override fun onClusterSelected(@NotNull cluster: IClusterDetail) {
        super.onClusterSelected(cluster)
        cosmosServerlessSparkSubmissionContentPanel.setEventsLocationPrefix(cluster.name)
    }

    override fun getData(@NotNull data: SparkSubmitModel) {
        super.getData(data)
        val sparkEventsPath = cosmosServerlessSparkSubmissionContentPanel.getEventsLocation().trim()
        (data as CosmosServerlessSparkSubmitModel).setSparkEventsDirectoryPath(sparkEventsPath)
    }

    override fun setData(@NotNull data: SparkSubmitModel) {
        super.setData(data)
        cosmosServerlessSparkSubmissionContentPanel.setEventsLocation((data as CosmosServerlessSparkSubmitModel).getSparkEventsDirectoryPath())
    }

    override fun getComponent(): JComponent {
        return cosmosServerlessSparkSubmissionContentPanel
    }
}