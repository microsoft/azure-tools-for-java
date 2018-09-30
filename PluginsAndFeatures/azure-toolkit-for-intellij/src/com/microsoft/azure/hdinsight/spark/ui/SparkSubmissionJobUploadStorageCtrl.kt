/**
 * Copyright (c) Microsoft Corporation
 *
 *
 * All rights reserved.
 *
 *
 * MIT License
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.ui

import com.microsoft.azure.hdinsight.common.ClusterManagerEx
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitJobUploadStorageModel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager
import com.microsoft.tooling.msservices.model.storage.BlobContainer
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import rx.Observable
import rx.schedulers.Schedulers
import java.awt.CardLayout
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.ItemEvent
import javax.swing.DefaultComboBoxModel

abstract class SparkSubmissionJobUploadStorageCtrl(val view: SparkSubmissionJobUploadStorageWithUploadPathPanel) : ILogger {
    val isCheckPassed
        get() = StringUtils.isEmpty(resultMessage)
    val resultMessage
        get() = view.storagePanel.errorMessage

    abstract fun getClusterName(): String?

    init {
        // refresh containers after account and key focus lost
        arrayOf(view.storagePanel.azureBlobCard.storageAccountField, view.storagePanel.azureBlobCard.storageKeyField).forEach {
            it.addFocusListener(object : FocusAdapter() {
                override fun focusLost(e: FocusEvent?) {
                    refreshContainers().subscribe(
                            { },
                            { err -> log().info(ExceptionUtils.getStackTrace(err)) })
                }
            })
        }
        // after container is selected, update upload path
        view.storagePanel.azureBlobCard.storageContainerComboBox.addItemListener { itemEvent ->
            if (itemEvent?.stateChange == ItemEvent.SELECTED) {
                updateStorageAfterContainerSelected().subscribe(
                        { },
                        { err -> log().info(ExceptionUtils.getStackTrace(err)) })
            }
        }
        // validate storage info after storage type is selected
        view.storagePanel.storageTypeComboBox.addItemListener { itemEvent ->
            // change panel
            val curLayout = view.storagePanel.storageCardsPanel.layout as CardLayout
            curLayout.show(view.storagePanel.storageCardsPanel, itemEvent.item as String)
            // validate storage info
            if (itemEvent?.stateChange == ItemEvent.SELECTED) {
                val selectedItem = itemEvent.item as String
                validateStorageInfoWhenStorageTypeSelected(selectedItem).subscribe(
                        { },
                        { err -> log().info(ExceptionUtils.getStackTrace(err)) })
            }
        }
    }

    private fun getClusterDetail(): IClusterDetail? {
        if (StringUtils.isEmpty(getClusterName())) {
            return null
        }
        return ClusterManagerEx.getInstance().getClusterDetailByName(getClusterName()).orElse(null)
    }

    private fun validateStorageInfoWhenStorageTypeSelected(selectedItem: String?): Observable<SparkSubmitJobUploadStorageModel> {
        return Observable.just(SparkSubmitJobUploadStorageModel())
                .doOnNext(view::getData)
                .observeOn(Schedulers.io())
                .map { toUpdate ->
                    when (selectedItem) {
                        StorageType.SparkInteractiveSession.title -> toUpdate.apply {
                            if (getClusterDetail() != null) {
                                errorMsg = null
                                storageAccountType = SparkSubmitStorageType.SparkInteractiveSession.toString()
                                uploadPath = "/SparkSubmission/"
                            } else {
                                errorMsg = "cluster not exist"
                                uploadPath = "-"
                            }
                        }
                        StorageType.ClusterDefaultStorage.title -> {
                            val clusterDetail = getClusterDetail()
                            if (clusterDetail == null) {
                                toUpdate.apply {
                                    errorMsg = "cluster not exist"
                                    uploadPath = "-"
                                }
                            } else {
                                try {
                                    clusterDetail.getConfigurationInfo()
                                    val defaultStorageAccount = clusterDetail.storageAccount
                                    val defaultStorageContainer = defaultStorageAccount.defaultContainerOrRootPath
                                    if (defaultStorageAccount != null && defaultStorageContainer != null) {
                                        toUpdate.apply {
                                            errorMsg = null
                                            uploadPath = getAzureBlobStoragePath(defaultStorageContainer, defaultStorageAccount.name)
                                            storageAccountType = SparkSubmitStorageType.DefaultStorageAccount.toString()
                                        }
                                    } else {
                                        toUpdate.apply {
                                            errorMsg = "cluster have no storage account or storage container"
                                            uploadPath = "-"
                                        }
                                    }
                                } catch (ex: Exception) {
                                    log().warn("error getting cluster storage configuration. " + ExceptionUtils.getStackTrace(ex))
                                    toUpdate.apply {
                                        errorMsg = "error getting cluster storage configuration"
                                        uploadPath = "-"
                                    }
                                }
                            }
                        }
                        StorageType.AzureBlob.title -> toUpdate.apply {
                            if (containersModel.size ==0 || containersModel.selectedItem == null) {
                                uploadPath = "-"
                                errorMsg = "azure blob storage form is not completed"
                            } else {
                                uploadPath = getAzureBlobStoragePath(storageAccount, containersModel.selectedItem as String)
                                errorMsg = null
                            }
                            storageAccountType = SparkSubmitStorageType.Blob.toString()
                        }
                        else -> toUpdate
                    }
                }
                .doOnNext { data ->
                    if (data.errorMsg != null) {
                        log().info("After select storage type, validate storage info error: " + data.errorMsg)
                    }
                    view.setData(data)
                }
    }

    fun refreshContainers(): Observable<SparkSubmitJobUploadStorageModel> {
        return Observable.just(SparkSubmitJobUploadStorageModel())
                .doOnNext(view::getData)
                .observeOn(Schedulers.io())
                .map { toUpdate ->
                    if (StringUtils.isEmpty(toUpdate.storageAccount) || StringUtils.isEmpty(toUpdate.storageKey)) {
                        toUpdate.apply { errorMsg = "Storage account and key can't be empty" }
                    } else {
                        try {
                            val clientStorageAccount = ClientStorageAccount(toUpdate.storageAccount)
                                    .apply { primaryKey = toUpdate.storageKey }
                            val containers = StorageClientSDKManager
                                    .getManager()
                                    .getBlobContainers(clientStorageAccount.connectionString)
                                    .map(BlobContainer::getName)
                                    .toTypedArray()
                            if (containers.isNotEmpty()) {
                                toUpdate.apply {
                                    containersModel = DefaultComboBoxModel(containers)
                                    containersModel.selectedItem = containersModel.getElementAt(0)
                                    selectedContainer = containersModel.getElementAt(0)
                                    uploadPath = getAzureBlobStoragePath(storageAccount, selectedContainer)
                                    errorMsg = null
                                }
                            } else {
                                toUpdate.apply { errorMsg = "no container found in this storage account" }
                            }
                        } catch (ex: Exception) {
                            log().info("Refresh azure blob contains error. " + ExceptionUtils.getStackTrace(ex))
                            toUpdate.apply { errorMsg = "Can't get storage containers, check if the key matches" }
                        }
                    }
                }
                .doOnNext { data ->
                    if (data.errorMsg != null) {
                        log().info("Refresh azure blob containers error: " + data.errorMsg)
                    }
                    view.setData(data)
                }
    }

    private fun updateStorageAfterContainerSelected(): Observable<SparkSubmitJobUploadStorageModel> {
        return Observable.just(SparkSubmitJobUploadStorageModel())
                .doOnNext(view::getData)
                .observeOn(Schedulers.io())
                .map { toUpdate ->
                    if (toUpdate.containersModel.size == 0) {
                        toUpdate.apply { errorMsg = "storage account has no containers" }

                    } else {
                        toUpdate.apply {
                            val selectedContainer = toUpdate.containersModel.selectedItem as String
                            uploadPath = getAzureBlobStoragePath(storageAccount, selectedContainer)
                            errorMsg = null
                        }
                    }
                }
                .doOnNext { data ->
                    if (data.errorMsg != null) {
                        log().info("Update storage info after container selected error: " + data.errorMsg)
                    }
                    view.setData(data)
                }
    }

    private fun getAzureBlobStoragePath(storageAccountName: String?, container: String?): String? {
        return if (StringUtils.isEmpty(storageAccountName) || StringUtils.isEmpty(container)) null else
            "wasbs://$container@${ClusterManagerEx.getInstance().getBlobFullName(storageAccountName)}/SparkSubmission/"
    }
}