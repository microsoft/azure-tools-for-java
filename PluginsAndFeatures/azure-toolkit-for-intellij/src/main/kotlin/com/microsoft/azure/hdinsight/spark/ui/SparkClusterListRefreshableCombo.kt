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

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.ui.SimpleListCellRenderer
import com.microsoft.azure.hdinsight.common.ClusterManagerEx
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.common.viewmodels.ComboBoxSelectionDelegated
import com.microsoft.azure.hdinsight.common.viewmodels.ComponentWithBrowseButtonEnabledDelegated
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosClusterManager
import com.microsoft.azure.hdinsight.spark.service.SparkClustersServices.arcadiaSparkClustersRefreshed
import com.microsoft.azure.hdinsight.spark.service.SparkClustersServices.arisSparkClustersRefreshed
import com.microsoft.azure.hdinsight.spark.service.SparkClustersServices.cosmosServerlessSparkAccountsRefreshed
import com.microsoft.azure.hdinsight.spark.service.SparkClustersServices.cosmosSparkClustersRefreshed
import com.microsoft.azure.hdinsight.spark.service.SparkClustersServices.hdinsightSparkClustersRefreshed
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkComputeManager
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail
import com.microsoft.intellij.forms.dsl.panel
import com.microsoft.intellij.rxjava.DisposableObservers
import com.microsoft.intellij.rxjava.IdeaSchedulers
import com.microsoft.intellij.ui.util.findFirst
import rx.Observable
import rx.Observable.*
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import java.awt.Font
import java.awt.event.ItemEvent
import java.util.concurrent.TimeUnit
import javax.swing.ComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JList

open class SparkClusterListRefreshableCombo: ILogger, Disposable {
    open fun getComboBoxNamePrefix(): String = "clusterListComboBox"

    private val clustersSelection  = ComboboxWithBrowseButton(JComboBox<IClusterDetail>(ImmutableComboBoxModel.empty())).apply {
        comboBox.name = getComboBoxNamePrefix() + "Combo"
        button.name = getComboBoxNamePrefix() + "Button"

        setButtonIcon(AllIcons.Actions.Refresh)

        comboBox.apply {
            setRenderer(object : SimpleListCellRenderer<IClusterDetail>() {
                override fun customize(list: JList<out IClusterDetail>,
                                       cluster: IClusterDetail?,
                                       index: Int,
                                       selected: Boolean,
                                       hasFocus: Boolean) {
                    font = if (cluster != null) {
                        setText(cluster.title)
                        font.deriveFont(Font.PLAIN)
                    } else {
                        setText((viewModel.toSelectClusterByIdBehavior.value as? String)
                                ?.takeIf { it.isNotBlank() }
                                ?.let { "$it (saved in configuration${if (comboBox.itemCount > 0) ", but not found" else ""})" }
                                ?: "<No selection>")
                        font.deriveFont(Font.ITALIC)
                    }
                }
            })

            addItemListener { event ->
                when (event.stateChange) {
                    ItemEvent.SELECTED -> if (event.item != null) {
                        viewModel.toSelectClusterByIdBehavior.onNext(
                                viewModel.clusterIdMapper(event.item as IClusterDetail))
                    }
                }
            }
        }

        button.apply {
            toolTipText = "Refresh"
            addActionListener {
                viewModel.doRefreshSubject.onNext(true)
            }
        }
    }

    val component: JComponent by lazy {
        val formBuilder = panel {
            row { c(clustersSelection) }
        }

        formBuilder.buildPanel()
    }

    open inner class ViewModel(private val initClusters: Array<IClusterDetail>,
                               val clusterIdMapper: (IClusterDetail?) -> String? = { cluster -> cluster?.clusterIdForConfiguration })
        : DisposableObservers() {

        val clusterListModelBehavior: BehaviorSubject<ImmutableComboBoxModel<IClusterDetail>> = disposableSubjectOf {
            BehaviorSubject.create(ImmutableComboBoxModel(initClusters))
        }

        val toSelectClusterByIdBehavior: BehaviorSubject<Any> = disposableSubjectOf { BehaviorSubject.create() }

        val doRefreshSubject: PublishSubject<Boolean> = disposableSubjectOf { PublishSubject.create() }

        private var isRefreshButtonEnabled: Boolean by ComponentWithBrowseButtonEnabledDelegated(clustersSelection)

        @Suppress("UNCHECKED_CAST")
        private val clusterComboBoxSelection: IClusterDetail? by ComboBoxSelectionDelegated(
                clustersSelection.comboBox as JComboBox<IClusterDetail>)

        // Only getter here since the select setter has a special behavior
        open val clusterDetailsWithRefresh: Observable<out List<IClusterDetail>>
                get() = hdinsightSparkClustersRefreshed

        val selectedCluster: IClusterDetail?
            get() = clusterListModelBehavior.value.selectedItem as? IClusterDetail

        // Monitoring cluster selection
        val clusterIsSelected: Observable<IClusterDetail?>
            get() = interval(200, TimeUnit.MILLISECONDS)
                    .takeUntil { Disposer.isDisposed(this) }
                    .map { clusterComboBoxSelection }
                    .distinctUntilChanged()
                    .doOnNext { if (it == null) {
                        doRefreshSubject.onNext(true)
                    }}
                    .distinctUntilChanged()
                    .doOnNext { log().info("Selected ${clusterIdMapper(it)}, (you may get duplicated outputs for each subscriptions)") }

        init {
            // To select cluster with refresh
            combineLatest(clusterListModelBehavior.distinctUntilChanged(),
                                     toSelectClusterByIdBehavior.distinctUntilChanged()) {
                clustersModel, clusterId -> clustersModel.apply { selectedItem = findClusterById(clustersModel, clusterId) }
            }.subscribe(
                    {
                        @Suppress("UNCHECKED_CAST")
                        this@SparkClusterListRefreshableCombo.clustersSelection.comboBox.model = it as ComboBoxModel<Any>
                    },
                    { log().warn("Can't get cluster list model", it) }
            )

            val myIdeaSchedulers = IdeaSchedulers(null)

            // Refreshing behavior
            doRefreshSubject
                    .throttleWithTimeout(200, TimeUnit.MILLISECONDS)
                    .filter { it } // TODO: If to add cancelling operation, remove the filter please
                    .observeOn(myIdeaSchedulers.dispatchUIThread())
                    .doOnNext { isRefreshButtonEnabled = false }
                    .observeOn(IdeaSchedulers(null).processBarVisibleAsync("Refreshing Spark clusters list"))
                    .flatMap { clusterDetailsWithRefresh }
                    .observeOn(myIdeaSchedulers.dispatchUIThread())
                    .doOnEach { isRefreshButtonEnabled = true }
                    .subscribe(
                            { clusterListModelBehavior.onNext(ImmutableComboBoxModel(it.toTypedArray())) },
                            { log().warn("Refresh cluster failure", it) }
                    )

        }

        open fun findClusterById(clustersModel: ComboBoxModel<IClusterDetail>, id: Any?): IClusterDetail? {
            return clustersModel.findFirst { clusterIdMapper(it) == id as? String }
        }
    }

    open val viewModel: ViewModel = ViewModel(ClusterManagerEx.getInstance().cachedClusters
            .filter { ClusterManagerEx.getInstance().hdInsightClusterFilterPredicate.test(it) }
            .toTypedArray()).apply { Disposer.register(this@SparkClusterListRefreshableCombo, this@apply) }

    override fun dispose() {
    }
}

class CosmosSparkClustersCombo: SparkClusterListRefreshableCombo() {
    inner class ViewModel
        :  SparkClusterListRefreshableCombo.ViewModel(AzureSparkCosmosClusterManager.getInstance().clusters
            .toTypedArray()) {

        override val clusterDetailsWithRefresh: Observable<out List<IClusterDetail>>
                get() = cosmosSparkClustersRefreshed
    }

    override val viewModel = ViewModel().apply { Disposer.register(this@CosmosSparkClustersCombo, this@apply) }
}

class ArisSparkClusterListRefreshableCombo: SparkClusterListRefreshableCombo() {
    inner class ViewModel
        : SparkClusterListRefreshableCombo.ViewModel(ClusterManagerEx.getInstance().cachedClusters
            .filterIsInstance<SqlBigDataLivyLinkClusterDetail>()
            .toTypedArray()) {

        override val clusterDetailsWithRefresh: Observable<out List<IClusterDetail>>
                get() = arisSparkClustersRefreshed
    }

    override val viewModel = ViewModel().apply { Disposer.register(this@ArisSparkClusterListRefreshableCombo, this@apply) }
}

class CosmosServerlessSparkAccountsCombo: SparkClusterListRefreshableCombo() {
    override fun getComboBoxNamePrefix(): String = "accountListComboBox"

    inner class ViewModel
        :  SparkClusterListRefreshableCombo.ViewModel(AzureSparkCosmosClusterManager.getInstance().accounts
            .toTypedArray()) {

        override val clusterDetailsWithRefresh: Observable<out List<IClusterDetail>>
                get() = cosmosServerlessSparkAccountsRefreshed
    }

    override val viewModel: SparkClusterListRefreshableCombo.ViewModel by lazy { ViewModel() }
}

class ArcadiaSparkClusterListRefreshableCombo: SparkClusterListRefreshableCombo() {
    override fun getComboBoxNamePrefix(): String = "computeListComboBox"

    inner class ViewModel
        : SparkClusterListRefreshableCombo.ViewModel(ArcadiaSparkComputeManager.getInstance().clusters
            .toTypedArray()) {
        override val clusterDetailsWithRefresh: Observable<out List<IClusterDetail>>
                get() = arcadiaSparkClustersRefreshed
    }

    override val viewModel = ViewModel().apply { Disposer.register(this@ArcadiaSparkClusterListRefreshableCombo, this@apply) }
}
