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

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.uiDesigner.core.GridConstraints.*
import com.microsoft.azure.hdinsight.common.AbfsUri
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.common.mvc.IdeaSettableControlWithRwLock
import com.microsoft.azure.hdinsight.common.mvvm.Mvvm
import com.microsoft.azure.hdinsight.common.viewmodels.ComboBoxSelectionDelegated
import com.microsoft.azure.hdinsight.common.viewmodels.ImmutableComboBoxModelDelegated
import com.microsoft.azure.hdinsight.sdk.cluster.AzureAdAccountDetail
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.sdk.common.ADLSGen2OAuthHttpObservable
import com.microsoft.azure.hdinsight.sdk.common.SharedKeyHttpObservable
import com.microsoft.azure.hdinsight.sdk.storage.ADLSGen2StorageAccount
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitJobUploadStorageModel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType.*
import com.microsoft.azure.hdinsight.spark.ui.ImmutableComboBoxModel.Companion.empty
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionJobUploadStorageBasicCard.StorageCheckEvent.SelectedStorageTypeEvent
import com.microsoft.azure.hdinsight.spark.ui.filesystem.ADLSGen2FileSystem
import com.microsoft.azure.hdinsight.spark.ui.filesystem.AdlsGen2VirtualFile
import com.microsoft.azure.hdinsight.spark.ui.filesystem.AzureStorageVirtualFile
import com.microsoft.azure.hdinsight.spark.ui.filesystem.AzureStorageVirtualFileSystem
import com.microsoft.intellij.forms.dsl.panel
import com.microsoft.intellij.rxjava.DisposableObservers
import com.microsoft.intellij.rxjava.IdeaSchedulers
import com.microsoft.intellij.ui.util.UIUtils.assertInDispatchThread
import com.microsoft.intellij.ui.util.iterator
import org.apache.commons.lang3.StringUtils
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subjects.ReplaySubject
import java.awt.CardLayout
import java.awt.event.ItemEvent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel

open class SparkSubmissionJobUploadStoragePanel
    : Mvvm, IdeaSettableControlWithRwLock<SparkSubmissionJobUploadStoragePanel.Model>, Disposable, ILogger {
    interface Model: Mvvm.Model,
            SparkSubmissionJobUploadStorageAzureBlobCard.Model,
            SparkSubmissionJobUploadStorageAdlsCard.Model,
            SparkSubmissionJobUploadStorageGen2Card.Model,
            SparkSubmissionJobUploadStorageGen2OAuthCard.Model,
            SparkSubmissionJobUploadStorageWebHdfsCard.Model {
        var storageAccountType: SparkSubmitStorageType?
    }

    private val storageTypeLabel = JLabel("Storage Type")
    private val adlsGen2Card = SparkSubmissionJobUploadStorageGen2Card()
    private val adlsGen2OAuthCard = SparkSubmissionJobUploadStorageGen2OAuthCard()

    private val adlsCard = SparkSubmissionJobUploadStorageAdlsCard()
    private val webHdfsCard = SparkSubmissionJobUploadStorageWebHdfsCard()
    private val storageTypeComboBox = ComboBox<SparkSubmitStorageType>(empty()).apply {
        name = "storageTypeComboBox"
        // validate storage info after storage type is selected
        addItemListener { itemEvent ->
            // change panel
            val curLayout = storageCardsPanel.layout as? CardLayout ?: return@addItemListener

            if (itemEvent?.stateChange == ItemEvent.SELECTED) {
                val selectedType = itemEvent.item as? SparkSubmitStorageType ?: return@addItemListener

                curLayout.show(storageCardsPanel, selectedType.description)

                // Send storage check event to current selected card
                viewModel.currentCardViewModel?.storageCheckSubject?.onNext(SelectedStorageTypeEvent(selectedType))
            }
        }

        renderer = object: SimpleListCellRenderer<SparkSubmitStorageType>() {
            override fun customize(list: JList<out SparkSubmitStorageType>?, type: SparkSubmitStorageType?, index: Int, selected: Boolean, hasFocus: Boolean) {
                text = type?.description ?: "<No storage type selected>"
            }
        }
    }

    private val storageCards = mapOf(
            ADLA_ACCOUNT_DEFAULT_STORAGE to SparkSubmissionJobUploadStorageAccountDefaultStorageCard(),
            BLOB to SparkSubmissionJobUploadStorageAzureBlobCard(),
            SPARK_INTERACTIVE_SESSION to SparkSubmissionJobUploadStorageSparkInteractiveSessionCard(),
            DEFAULT_STORAGE_ACCOUNT to SparkSubmissionJobUploadStorageClusterDefaultStorageCard(),
            NOT_SUPPORT_STORAGE_TYPE to SparkSubmissionJobUploadStorageClusterNotSupportStorageCard(),
            ADLS_GEN1 to adlsCard,
            ADLS_GEN2 to adlsGen2Card,
            ADLS_GEN2_FOR_OAUTH to adlsGen2OAuthCard,
            WEBHDFS to webHdfsCard
    )

    private val storageCardsPanel = JPanel(CardLayout()).apply {
        storageCards.forEach { (_, card) -> add(card.view, card.title) }
    }

    override val view by lazy {
        val formBuilder = panel {
            columnTemplate {
                col {
                    anchor = ANCHOR_WEST
                }
                col {
                    anchor = ANCHOR_WEST
                    hSizePolicy = SIZEPOLICY_WANT_GROW
                    fill = FILL_HORIZONTAL
                }
            }
            row {
                c(storageTypeLabel.apply { labelFor = storageTypeComboBox }) { indent = 2 }
                        c(storageTypeComboBox) { indent = 3 }
            }
            row {
                c(storageCardsPanel) { indent = 2; colSpan = 2; hSizePolicy = SIZEPOLICY_WANT_GROW; fill = FILL_HORIZONTAL}
            }
        }

        formBuilder.buildPanel()
    }

    open inner class ViewModel : Mvvm.ViewModel, DisposableObservers() {
        var deployStorageTypeSelection: SparkSubmitStorageType? by ComboBoxSelectionDelegated(storageTypeComboBox)
        var deployStorageTypesModel: ImmutableComboBoxModel<SparkSubmitStorageType> by ImmutableComboBoxModelDelegated(storageTypeComboBox)

        val validatedStorageUploadUri: BehaviorSubject<String> = disposableSubjectOf {  BehaviorSubject.create() }

        // In order to get the pre select cluster name, use Replay Subject type
        private val clusterSelectedCapacity = 2
        val clusterSelectedSubject: ReplaySubject<IClusterDetail> = disposableSubjectOf {
            ReplaySubject.createWithSize(clusterSelectedCapacity)
        }

        val currentCardViewModel
            get() = storageCards[deployStorageTypeSelection]?.viewModel

        // Update storage type selection combo
        private fun updateStorageTypesModelForCluster(clusterDetail: IClusterDetail?): SparkSubmitStorageType? {
            assertInDispatchThread()

            if (clusterDetail == null) {
                return deployStorageTypeSelection
            }

            val optionTypes = clusterDetail.storageOptionsType.optionTypes
            val currentStorageTypesModel = deployStorageTypesModel
            val newStorageTypesModel = ImmutableComboBoxModel(optionTypes)

            val storageTypesModelToSet = if (currentStorageTypesModel.size == optionTypes.size) {
                // Deep compare the size and items
                var isDeepEqualed = true
                for (i in 0 until currentStorageTypesModel.size) {
                    if (currentStorageTypesModel.getElementAt(i) != optionTypes[i]) {
                        isDeepEqualed = false
                        break
                    }
                }

                if (isDeepEqualed) currentStorageTypesModel else newStorageTypesModel
            } else newStorageTypesModel

            if (storageTypesModelToSet != currentStorageTypesModel) {
                // there exist 4 cases to set the storage type
                // 1.select cluster -> set to default
                // 2.reload config with not null type -> set to saved type
                // 3.reload config with null storage type -> set to default
                // 4.create config  -> set to default
                deployStorageTypesModel = storageTypesModelToSet
                deployStorageTypeSelection = clusterDetail.defaultStorageType
            }

            return storageTypesModelToSet.selectedItem as? SparkSubmitStorageType ?: clusterDetail.defaultStorageType
        }

        val errorMessage: String?
            get() {
                val currentCardViewModel = this.currentCardViewModel
                        ?: return "No selected artifacts uploading storage type"

                return currentCardViewModel.errorMessage
            }

        private val ideaSchedulers = IdeaSchedulers()

        init {
            // Merge all storage cards storage validated upload URI events into the panel's
            Observable.merge(storageCards.values.map { it.viewModel.validatedStorageUploadUri })
                      .subscribe(validatedStorageUploadUri::onNext,
                                 validatedStorageUploadUri::onError,
                                 validatedStorageUploadUri::onCompleted)

            clusterSelectedSubject
                    .observeOn(ideaSchedulers.dispatchUIThread())
                    .doOnNext {
                        log().info("Current model storage account type is $deployStorageTypeSelection")
                        updateStorageTypesModelForCluster(it)
                        log().info("Update model storage account type to $deployStorageTypeSelection")
                    }
                    .observeOn(ideaSchedulers.dispatchPooledThread())
                    .subscribe(
                            { currentCardViewModel?.cluster = it },
                            { log().warn("Failed to update storage type model: ${it.message}") }
                    )
        }

        fun prepareVFSRoot(uploadRootPathUri: AbfsUri, storageAccount: IHDIStorageAccount?, cluster: IClusterDetail?): AzureStorageVirtualFile? {
            var account: String? = null
            var accessKey: String? = null
            var fsType: AzureStorageVirtualFileSystem.VFSSupportStorageType? = null
            try {
                when (viewModel.deployStorageTypeSelection) {
                    DEFAULT_STORAGE_ACCOUNT -> {
                        when (storageAccount) {
                            is ADLSGen2StorageAccount  -> {
                                fsType = AzureStorageVirtualFileSystem.VFSSupportStorageType.ADLSGen2
                                account = storageAccount.name
                                accessKey = storageAccount.primaryKey
                            }
                        }
                    }

                    ADLS_GEN2 -> {
                        fsType = AzureStorageVirtualFileSystem.VFSSupportStorageType.ADLSGen2
                        account = uploadRootPathUri.accountName
                        accessKey = adlsGen2Card.storageKeyField.text.trim()
                    }

                    ADLS_GEN2_FOR_OAUTH -> {
                        fsType = AzureStorageVirtualFileSystem.VFSSupportStorageType.ADLSGen2
                        account = uploadRootPathUri.accountName
                    }

                    else -> {
                    }
                }
            } catch (ex: IllegalArgumentException) {
                log().warn("Preparing file system encounter ", ex)
            }

            when (fsType) {
                AzureStorageVirtualFileSystem.VFSSupportStorageType.ADLSGen2 -> {
                    // for issue #3159, upload path maybe not ready if switching cluster fast so path is the last cluster's path
                    // if switching between gen2 clusters, need to check account is matched
                    if (uploadRootPathUri.accountName != account) {
                        return null
                    }

                    // Prepare HttpObservable for different cluster type
                    val http =
                        if (cluster is AzureAdAccountDetail) {
                            // Use Azure AD account to access storage data corresponding to Synapse Spark pool.
                            // In this way at least "Storage Blob Data Reader" role is required, or else we will get
                            // HTTP 403 Error when list files on the storage.
                            // https://docs.microsoft.com/en-us/azure/storage/common/storage-access-blobs-queues-portal
                            ADLSGen2OAuthHttpObservable(cluster.tenantId)
                        } else {
                            if (StringUtils.isBlank(accessKey)) {
                                return null
                            }
                            // Use account access key to access Gen2 storage data corresponding to
                            // HDInsight/Mfa/Linked HDInsight cluster. In this way at least "Storage Account Contributor"
                            // role is required, or else we will get HTTP 403 Error when list files on the storage
                            // https://docs.microsoft.com/en-us/azure/storage/common/storage-access-blobs-queues-portal
                            SharedKeyHttpObservable(account, accessKey)
                        }

                    val fileSystem = ADLSGen2FileSystem(http, uploadRootPathUri)
                    return AdlsGen2VirtualFile(uploadRootPathUri, true, fileSystem)
                }
                else -> {
                    return null
                }
            }
        }
    }

    override val viewModel = ViewModel().apply {
        Disposer.register(this@SparkSubmissionJobUploadStoragePanel, this@apply)
        storageCards.values.forEach { card -> Disposer.register(this@SparkSubmissionJobUploadStoragePanel, card) }
    }

    override val model: Model
        get() = SparkSubmitJobUploadStorageModel().apply { getData(this) }

    override fun dispose() {
    }

    override fun readWithLock(to: Model) {
        viewModel.deployStorageTypesModel.iterator.forEach {
            storageCards[it]?.readWithLock(to)
        }

        to.storageAccountType = viewModel.deployStorageTypesModel.selectedItem as? SparkSubmitStorageType
        to.errorMsg = viewModel.currentCardViewModel?.errorMessage
    }

    override fun writeWithLock(from: Model) {
        viewModel.apply {
            if (deployStorageTypeSelection != from.storageAccountType) {
                if (deployStorageTypesModel.size == 0) {
                    deployStorageTypesModel = ImmutableComboBoxModel(
                            from.storageAccountType?.let { arrayOf(it) } ?: emptyArray())
                }

                deployStorageTypeSelection = from.storageAccountType
            }
        }

        viewModel.deployStorageTypesModel.iterator.forEach {
            storageCards[it]?.writeWithLock(from)
        }
    }
}