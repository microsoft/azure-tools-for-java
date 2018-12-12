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
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightLivyLinkClusterDetail
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.sdk.common.AzureSparkClusterManager
import com.microsoft.azure.hdinsight.sdk.common.HDIException
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster
import com.microsoft.azure.hdinsight.sdk.storage.ADLSStorageAccount
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount
import com.microsoft.azure.hdinsight.spark.common.SparkBatchJob
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitJobUploadStorageModel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager
import com.microsoft.tooling.msservices.model.storage.BlobContainer
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import java.awt.CardLayout
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.ItemEvent
import java.util.concurrent.TimeUnit
import javax.swing.DefaultComboBoxModel

abstract class SparkSubmissionJobUploadStorageCtrl(val view: SparkSubmissionJobUploadStorageWithUploadPathPanel) : ILogger {
    val isCheckPassed
        get() = StringUtils.isEmpty(resultMessage)
    val resultMessage
        get() = view.storagePanel.errorMessage

    //storage check event for storageCheckSubject in panel
    open class StorageCheckEvent(val message: String)
    class StorageCheckSelectedClusterEvent(val clusterName: String) : StorageCheckEvent("Selected cluster $clusterName")
    class StorageCheckSignInOutEvent() : StorageCheckEvent("After user clicked sign in/off in ADLS Gen 1 storage type")
    class StorageCheckPathFocusLostEvent(val rootPathType: String) : StorageCheckEvent("$rootPathType root path focus lost")
    class StorageCheckSelectedStorageTypeEvent(val storageType: String) : StorageCheckEvent("Selected storage type: $storageType")

    abstract fun getClusterName(): String?

    init {
        // check storage info when cluster selection changes
        registerStorageInfoCheck()

        // refresh containers after account and key focus lost
        arrayOf(view.storagePanel.azureBlobCard.storageAccountField, view.storagePanel.azureBlobCard.storageKeyField).forEach {
            it.addFocusListener(object : FocusAdapter() {
                override fun focusLost(e: FocusEvent?) {
                    if (view.storagePanel.azureBlobCard.storageContainerUI.button.isEnabled) {
                        view.storagePanel.azureBlobCard.storageContainerUI.button.isEnabled = false
                        refreshContainers()
                            .doOnEach { view.storagePanel.azureBlobCard.storageContainerUI.button.isEnabled = true }
                            .subscribe(
                                { },
                                { err -> log().warn(ExceptionUtils.getStackTrace(err)) })
                    }
                }})
        }
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
            if (itemEvent?.stateChange == ItemEvent.SELECTED) {
                view.storageCheckSubject.onNext(StorageCheckSelectedStorageTypeEvent(itemEvent.item as String))
            }
        }

        // handle sign in/out action when sign in/out link is clicked
        arrayOf(view.storagePanel.adlsCard.signInCard.signInLink, view.storagePanel.adlsCard.signOutCard.signOutLink)
            .forEach {
                it.addActionListener {
                    AzureSignInAction.onAzureSignIn(null)
                    view.storageCheckSubject.onNext(StorageCheckSignInOutEvent())
                }
            }

        // validate storage info when ADLS Root Path field focus lost
        view.storagePanel.adlsCard.adlsRootPathField.addFocusListener( object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                view.storageCheckSubject.onNext(StorageCheckPathFocusLostEvent("ADLS"))
            }
        })

        // validate storage info when webhdfs root path field lost
        view.storagePanel.webHdfsCard.webHdfsRootPathField.addFocusListener( object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                view.storageCheckSubject.onNext(StorageCheckPathFocusLostEvent("WEBHDFS"))
            }
        })
    }

    fun selectCluster(clusterName: String) {
        view.storageCheckSubject.onNext(StorageCheckSelectedClusterEvent(clusterName))
    }

    private fun registerStorageInfoCheck(): Subscription = view.storageCheckSubject
            .groupBy { checkEvent -> checkEvent::class.java.typeName}
            .subscribe(
                    { groupedOb ->
                        groupedOb
                                .throttleWithTimeout(200, TimeUnit.MILLISECONDS)
                                .doOnNext { log().info("Receive checking message ${it.message}") }
                                .flatMap { validateStorageInfo(it) }
                                .subscribe()
                    },
                    { err -> log().warn(ExceptionUtils.getStackTrace(err)) })

    abstract fun getClusterDetail(): IClusterDetail?

    fun setDefaultStorageType(checkEvent:StorageCheckEvent) {
        synchronized(view.storagePanel) {
            if (checkEvent is StorageCheckSelectedClusterEvent) {

                //check cluster type then reset storage combox
                val defaultStorageTitle = view.storagePanel.clusterDefaultStorageCard.title
                val helpSessionTitle = view.storagePanel.sparkInteractiveSessionCard.title
                val webHdfsTitle = view.storagePanel.webHdfsCard.title
                val azureBlobTitle = view.storagePanel.azureBlobCard.title
                val adlsTitle = view.storagePanel.adlsCard.title

                val clusterDetail = getClusterDetail()
                view.storagePanel.storageTypeComboBox.selectedItem = null
                view.storagePanel.storageTypeComboBox.model = when (clusterDetail) {
                    is ClusterDetail ->{
                        //get storageaccount may get HDIExpcetion for null value
                        var storageAccount = try {
                            clusterDetail.storageAccount
                        } catch (igonred: HDIException) {
                            clusterDetail.getConfigurationInfo()
                            clusterDetail.storageAccount
                        }

                        ImmutableComboBoxModel(arrayOf(
                                defaultStorageTitle,
                                when (storageAccount) {
                                    is HDStorageAccount -> azureBlobTitle
                                    is ADLSStorageAccount, is AzureSparkServerlessCluster.StorageAccount -> adlsTitle
                                    else -> helpSessionTitle
                                })).apply {
                            selectedItem = defaultStorageTitle
                        }
                    }

                    is HDInsightLivyLinkClusterDetail, is HDInsightAdditionalClusterDetail -> ImmutableComboBoxModel(arrayOf(
                            azureBlobTitle,
                            adlsTitle,
                            helpSessionTitle)).apply {
                        selectedItem = helpSessionTitle
                    }

                    is SqlBigDataLivyLinkClusterDetail -> {
                        ImmutableComboBoxModel(arrayOf(
                                helpSessionTitle,
                                webHdfsTitle
                        )).apply {
                            selectedItem = helpSessionTitle
                        }
                    }

                    else -> ImmutableComboBoxModel(arrayOf(
                            defaultStorageTitle,
                            azureBlobTitle,
                            adlsTitle,
                            helpSessionTitle,
                            webHdfsTitle)).apply {
                        selectedItem = defaultStorageTitle
                    }
                }
            }
        }
    }

    private fun validateStorageInfo(checkEvent:StorageCheckEvent):
            Observable<SparkSubmitJobUploadStorageModel> {
        return Observable.just(SparkSubmitJobUploadStorageModel())
            .doOnNext(view::getData)
            .doOnNext { setDefaultStorageType(checkEvent) }
            // set error message to prevent user from applying the changes when validation is not completed
            .map {
                it.apply {
                    errorMsg = "validating storage info is not completed"
                }
            }
            .doOnNext(view::setData)
            .observeOn(Schedulers.io())
            .doOnNext(view::getData)
            .map {

                when (it.storageAccountType) {
                    SparkSubmitStorageType.SPARK_INTERACTIVE_SESSION -> it.apply {
                        if (getClusterDetail() != null) {
                            errorMsg = null
                            uploadPath = "/SparkSubmission/"
                        } else {
                            errorMsg = "Cluster not exist"
                            uploadPath = "-"
                        }
                    }
                    SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT -> it.apply {
                        val clusterDetail = getClusterDetail()
                        if (clusterDetail == null) {
                            errorMsg = "Cluster not exist"
                            uploadPath = "-"
                        } else {
                            try {
                                clusterDetail.getConfigurationInfo()
                                val defaultStorageAccount = clusterDetail.storageAccount
                                if (defaultStorageAccount == null) {
                                    errorMsg = "Cluster have no storage account"
                                    uploadPath = "-"
                                } else {
                                    val path = getUploadPath(defaultStorageAccount)
                                    if (path == null) {
                                        errorMsg = "Error getting upload path from storage account"
                                        uploadPath = "-"
                                    } else {
                                        errorMsg = null
                                        uploadPath = path
                                    }
                                }
                            } catch (ex: Exception) {
                                errorMsg = "Error getting cluster storage configuration"
                                uploadPath = "-"
                                log().warn(errorMsg + ". " + ExceptionUtils.getStackTrace(ex))
                            }
                        }
                    }
                    SparkSubmitStorageType.BLOB -> it.apply {
                        if (containersModel.size == 0 || containersModel.selectedItem == null) {
                            uploadPath = "-"
                            errorMsg = "Azure Blob storage form is not completed"
                        } else {
                            uploadPath = getAzureBlobStoragePath(ClusterManagerEx.getInstance().getBlobFullName(storageAccount), containersModel.selectedItem as String)
                            errorMsg = null
                        }
                    }
                    SparkSubmitStorageType.ADLS_GEN1 -> it.apply {
                        if (!AzureSparkClusterManager.getInstance().isSignedIn()) {
                            uploadPath = "-"
                            errorMsg = "ADLS Gen 1 storage type requires user to sign in first"
                        } else {
                            // basic validation for ADLS root path
                            // pattern for adl root path. e.g. adl://john.azuredatalakestore.net/root/path/
                            if (adlsRootPath != null && !SparkBatchJob.AdlsPathPattern.toRegex().matches(adlsRootPath!!)) {
                                uploadPath = "-"
                                errorMsg = "ADLS Root Path is invalid"
                            } else {
                                val formatAdlsRootPath = if (adlsRootPath?.endsWith("/") == true) adlsRootPath else "$adlsRootPath/"
                                uploadPath = "${formatAdlsRootPath}SparkSubmission/"
                                errorMsg = null
                            }
                        }
                    }
                    SparkSubmitStorageType.WEBHDFS -> it.apply {
                        //pattern for webhdfs root path.e.g http://host/webhdfs/v1/
                        val rootPath = webHdfsRootPath?.trim() ?: return@apply
                        if (!SparkBatchJob.WebHDFSPathPattern.toRegex().matches(rootPath)) {
                            uploadPath = "-"
                            errorMsg = "Webhdfs root path is not valid"
                        } else {
                            val formatWebHdfsRootPath = if (rootPath.endsWith("/") == true)
                                rootPath.trimEnd('/')
                            else rootPath

                            uploadPath = "${formatWebHdfsRootPath}/SparkSubmission/"

                            val clusterDetail = getClusterDetail()
                            when (clusterDetail) {
                                is SqlBigDataLivyLinkClusterDetail -> webHdfsAuthUser = clusterDetail.httpUserName
                                else -> webHdfsAuthUser = SparkSubmissionJobUploadWebHdfsSignOutCard.defaultAuthUser
                            }

                            errorMsg = null
                        }
                    }
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
            // set error message to prevent user from applying the change when refreshing is not completed
            .map { it.apply { errorMsg = "refreshing storage containers is not completed" } }
            .doOnNext(view::setData)
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
                                    uploadPath = getAzureBlobStoragePath(ClusterManagerEx.getInstance().getBlobFullName(storageAccount), selectedContainer)
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
            // set error message to prevent user from applying the change when updating is not completed
            .map { it.apply { "updating upload path is not completed" } }
            .doOnNext(view::setData)
            .observeOn(Schedulers.io())
            .map { toUpdate ->
                if (toUpdate.containersModel.size == 0) {
                    toUpdate.apply { errorMsg = "Storage account has no containers" }
                } else {
                    toUpdate.apply {
                        val selectedContainer = toUpdate.containersModel.selectedItem as String
                        uploadPath = getAzureBlobStoragePath(ClusterManagerEx.getInstance().getBlobFullName(storageAccount), selectedContainer)
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

    private fun getAzureBlobStoragePath(fullStorageBlobName: String?, container: String?): String? {
        return if (StringUtils.isBlank(fullStorageBlobName) || StringUtils.isBlank(container)) null else
            "wasbs://$container@$fullStorageBlobName/SparkSubmission/"
    }

    private fun getUploadPath(account: IHDIStorageAccount): String? =
            when (account) {
                is HDStorageAccount -> getAzureBlobStoragePath(account.fullStorageBlobName, account.defaultContainer)
                is ADLSStorageAccount ->
                    if (StringUtils.isBlank(account.name) || StringUtils.isBlank(account.defaultContainerOrRootPath)) null
                    else "adl://${account.name}.azuredatalakestore.net${account.defaultContainerOrRootPath}SparkSubmission/"
                is AzureSparkServerlessCluster.StorageAccount -> account.defaultContainerOrRootPath?.let { "${it}SparkSubmission/" }
                else -> null
            }
}