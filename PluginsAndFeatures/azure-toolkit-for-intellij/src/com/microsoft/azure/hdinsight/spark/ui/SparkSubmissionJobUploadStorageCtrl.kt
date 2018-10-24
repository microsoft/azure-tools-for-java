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
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitJobUploadStorageModel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager
import com.microsoft.tooling.msservices.model.storage.BlobContainer
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import java.awt.CardLayout
import java.awt.event.ItemEvent
import java.util.concurrent.TimeUnit
import javax.swing.DefaultComboBoxModel

abstract class SparkSubmissionJobUploadStorageCtrl(val view: SparkSubmissionJobUploadStorageWithUploadPathPanel) : ILogger {
    val isCheckPassed
        get() = StringUtils.isEmpty(resultMessage)
    val resultMessage
        get() = view.storagePanel.errorMessage

    abstract fun getClusterName(): String?

    init {
        // check storage info when cluster selection changes
        registerStorageInfoCheck()

        // refresh containers after refresh button is clicked
        view.storagePanel.azureBlobCard.storageContainerUI.button.addActionListener {
            if (view.storagePanel.azureBlobCard.storageContainerUI.button.isEnabled) {
                view.storagePanel.azureBlobCard.storageContainerUI.button.isEnabled = false
                refreshContainers()
                    .doOnEach { view.storagePanel.azureBlobCard.storageContainerUI.button.isEnabled = true }
                    .subscribe(
                        { },
                        { err -> log().warn(ExceptionUtils.getStackTrace(err)) })
            }
        }
        // after container is selected, update upload path
        view.storagePanel.azureBlobCard.storageContainerUI.comboBox.addItemListener { itemEvent ->
            if (itemEvent?.stateChange == ItemEvent.SELECTED) {
                updateStorageAfterContainerSelected().subscribe(
                        { },
                        { err -> log().warn(ExceptionUtils.getStackTrace(err)) })
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
                validateStorageInfo(selectedItem).subscribe(
                        { },
                        { err -> log().warn(ExceptionUtils.getStackTrace(err)) })
            }
        }
    }

    fun selectCluster(clusterName: String) {
        view.storageCheckSubject.onNext("Selected cluster $clusterName")
    }

    private fun registerStorageInfoCheck(): Subscription = view.storageCheckSubject
            .throttleWithTimeout(500, TimeUnit.MILLISECONDS)
            .doOnNext { log().debug("Receive checking message $it") }
            .flatMap { validateStorageInfo(view.storagePanel.storageTypeComboBox.selectedItem as String) }
            .subscribe(
                    { },
                    { err -> log().warn(ExceptionUtils.getStackTrace(err)) })

    abstract fun getClusterDetail(): IClusterDetail?

    private fun validateStorageInfo(selectedItem: String): Observable<SparkSubmitJobUploadStorageModel> {
        return Observable.just(SparkSubmitJobUploadStorageModel())
                .doOnNext(view::getData)
                .observeOn(Schedulers.io())
                .map { toUpdate ->
                    when (selectedItem) {
                        view.storagePanel.sparkInteractiveSessionCard.title -> toUpdate.apply {
                            if (getClusterDetail() != null) {
                                errorMsg = null
                                storageAccountType = SparkSubmitStorageType.SPARK_INTERACTIVE_SESSION
                                uploadPath = "/SparkSubmission/"
                            } else {
                                errorMsg = "Cluster not exist"
                                uploadPath = "-"
                            }
                        }
                        view.storagePanel.clusterDefaultStorageCard.title -> toUpdate.apply {
                            val clusterDetail = getClusterDetail()
                            if (clusterDetail == null) {
                                errorMsg = "Cluster not exist"
                                uploadPath = "-"
                            } else {
                                try {
                                    clusterDetail.getConfigurationInfo()
                                    val defaultStorageAccount = clusterDetail.storageAccount
                                    val defaultContainerOrRootPath = defaultStorageAccount.defaultContainerOrRootPath
                                    if (defaultStorageAccount != null && defaultContainerOrRootPath != null) {
                                        errorMsg = null
                                        uploadPath = if (clusterDetail is AzureSparkServerlessCluster) getAzureDataLakeStoragePath(defaultContainerOrRootPath) else getAzureBlobStoragePath(defaultStorageAccount.name, defaultContainerOrRootPath)
                                        storageAccountType = SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT
                                    } else {
                                        errorMsg = "Cluster have no storage account or storage container"
                                        uploadPath = "-"
                                    }
                                } catch (ex: Exception) {
                                    errorMsg = "Error getting cluster storage configuration"
                                    uploadPath = "-"
                                    log().warn(errorMsg + ". " + ExceptionUtils.getStackTrace(ex))
                                }
                            }
                        }
                        view.storagePanel.azureBlobCard.title -> toUpdate.apply {
                            if (containersModel.size == 0 || containersModel.selectedItem == null) {
                                uploadPath = "-"
                                errorMsg = "Azure Blob storage form is not completed"
                            } else {
                                uploadPath = getAzureBlobStoragePath(storageAccount, containersModel.selectedItem as String)
                                errorMsg = null
                            }
                            storageAccountType = SparkSubmitStorageType.BLOB
                        }
                        else -> toUpdate
                    }
                }
                .doOnNext { data ->
                    if (data.errorMsg != null) {
                        log().info("After selecting storage type, the storage info validation error is got: " + data.errorMsg)
                    }
                    view.setData(data)
                }
    }

    private fun refreshContainers(): Observable<SparkSubmitJobUploadStorageModel> {
        return Observable.just(SparkSubmitJobUploadStorageModel())
                .doOnNext(view::getData)
                .observeOn(Schedulers.io())
                .map { toUpdate ->
                    toUpdate.apply {
                        if (StringUtils.isEmpty(toUpdate.storageAccount) || StringUtils.isEmpty(toUpdate.storageKey)) {
                            errorMsg = "Storage account and key can't be empty"
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
                                    containersModel = DefaultComboBoxModel(containers)
                                    containersModel.selectedItem = containersModel.getElementAt(0)
                                    selectedContainer = containersModel.getElementAt(0)
                                    uploadPath = getAzureBlobStoragePath(storageAccount, selectedContainer)
                                    val credentialAccount = getCredentialAzureBlobAccount()
                                    credentialAccount?.let {
                                        view.secureStore?.savePassword(credentialAccount, storageAccount, storageKey) }
                                    errorMsg = null
                                } else {
                                    errorMsg = "No container found in this storage account"
                                }
                            } catch (ex: Exception) {
                                log().info("Refresh Azure Blob contains error. " + ExceptionUtils.getStackTrace(ex))
                                errorMsg = "Can't get storage containers, check if the key matches"
                            }
                        }
                    }
                }
                .doOnNext { data ->
                    if (data.errorMsg != null) {
                        log().info("Refresh Azure Blob containers error: " + data.errorMsg)
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
                        toUpdate.apply { errorMsg = "Storage account has no containers" }
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

    private fun getRootPathEndsWithSlash(rootPath: String): String {
        return if (rootPath.endsWith("/")) rootPath else "$rootPath/"
    }

    private fun getAzureDataLakeStoragePath(rootPath: String?): String? {
        return if (StringUtils.isEmpty(rootPath)) null else
            "${getRootPathEndsWithSlash(rootPath.orEmpty())}SparkSubmission/"
    }
}