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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.HideableTitledPanel
import com.intellij.uiDesigner.core.GridConstraints.*
import com.microsoft.azure.hdinsight.common.ClusterManagerEx
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.common.mvc.SettableControl
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.sdk.common.AzureSparkClusterManager
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccountType
import com.microsoft.azure.hdinsight.sdk.storage.StoragePathInfo
import com.microsoft.azure.hdinsight.spark.common.SparkBatchJob
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitJobUploadStorageModel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionJobUploadStorageCtrl.StorageCheckEvent
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail
import com.microsoft.azuretools.securestore.SecureStore
import com.microsoft.azuretools.service.ServiceManager
import com.microsoft.intellij.forms.dsl.panel
import com.microsoft.intellij.rxjava.DisposableObservers
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import rx.Observable
import rx.Observable.empty
import rx.Observable.just
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import rx.subjects.ReplaySubject
import java.awt.CardLayout
import java.util.concurrent.TimeUnit
import javax.swing.*
import javax.swing.event.DocumentEvent

class SparkSubmissionJobUploadStorageWithUploadPathPanel
    : JPanel(), Disposable, SettableControl<SparkSubmitJobUploadStorageModel>, ILogger {
    interface Control {
        val isCheckPassed: Boolean
        val resultMessage: String?
        fun getUploadPath(account: IHDIStorageAccount): String?
        fun getAzureBlobStoragePath(fullStorageBlobName: String?, container: String?, schema: String): String?
    }

    val secureStore: SecureStore? = ServiceManager.getServiceProvider(SecureStore::class.java)
    private val jobUploadStorageTitle = "Job Upload Storage"
    val invalidUploadPath = "<Invalid Upload Path>"
    private val unsupportAccountType = "<Storage Account Type Is Not Supported>"
    private val uploadPathLabel = JLabel("Upload Path")
    val uploadPathField = JTextField().apply {
        isEditable = false
        border = BorderFactory.createEmptyBorder()
    }

    val storagePanel = SparkSubmissionJobUploadStoragePanel().apply {
        Disposer.register(this@SparkSubmissionJobUploadStorageWithUploadPathPanel, this@apply)
    }

    private val hideableJobUploadStoragePanel = HideableTitledPanel(jobUploadStorageTitle, true, storagePanel, false)

    init {
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
                row {
                    c(uploadPathLabel) { indent = 0 }; c(uploadPathField) {}
                }
                row {
                    c(hideableJobUploadStoragePanel) { colSpan = 2; hSizePolicy = SIZEPOLICY_WANT_GROW; fill = FILL_HORIZONTAL }
                }
            }
        }

        layout = formBuilder.createGridLayoutManager()
        formBuilder.allComponentConstraints.forEach { (component, gridConstrains) -> add(component, gridConstrains) }
    }

    val control: Control = SparkSubmissionJobUploadStorageCtrl(this)

    inner class ViewModel : DisposableObservers() {
        val uploadPathFieldSubject:PublishSubject<String> = PublishSubject.create()

        val uploadStorage = storagePanel.viewModel.apply {
            // check storage info when cluster selection changes
            storageCheckSubject
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
        }

        val clusterSelectedCapacity = 2

        //in order to get the pre select cluster name, use replaysubject type
        val clusterSelectedSubject: ReplaySubject<IClusterDetail> = disposableSubjectOf {
            ReplaySubject.createWithSize(clusterSelectedCapacity)
        }

        private fun setDefaultStorageType(checkEvent: StorageCheckEvent,
                                          clusterDetail: IClusterDetail,
                                          model : SparkSubmitJobUploadStorageModel) {
            if (checkEvent is SparkSubmissionJobUploadStorageCtrl.StorageCheckSelectedClusterEvent) {
                val optionTypes = clusterDetail.storageOptionsType.optionTypes

                // if selection.selectedItem is null ,will trigger storage type combo box deselected event and
                // event.item is the model getSelectedItem which is model(0)
                // reset selectedItem will trigger deselected and selected event which will repaint the panel in setData
                uploadStorage.deployStorageTypeSelection = null

                // there exist 4 cases to set the storage type
                // 1.select cluster -> set to default
                // 2.reload config with not null type -> set to saved type
                // 3.reload config with null storage type -> set to default
                // 4.create config  -> set to default
                uploadStorage.deployStorageTypesModel = ImmutableComboBoxModel(optionTypes).apply {
                    if (checkEvent.preClusterName != null) {
                        // for case1, preClusterName is not null
                        model.storageAccountType = clusterDetail.defaultStorageType
                    } else {
                        // for case2, model.selectedItem has value since loading config can trigger setData
                        model.storageAccountType = viewModel.uploadStorage.deployStorageTypesModel.selectedItem as? SparkSubmitStorageType?
                    }

                    // for case3 && 4 ,both preClusterName and model.selectItem are null
                    if(model.storageAccountType == null){
                        model.storageAccountType = clusterDetail.defaultStorageType
                    }

                    selectedItem = model.storageAccountType
                }
            }

        }

        private fun setUploadInfoForLinkedCluster(cluster: IClusterDetail, model: SparkSubmitJobUploadStorageModel) {
            model.apply {
                val defaultRootPath = (cluster as? HDInsightAdditionalClusterDetail)?.defaultStorageRootPath
                defaultRootPath?.run {
                    val pathUri = StoragePathInfo(defaultRootPath).path
                    if (cluster.defaultStorageType == model.storageAccountType) {
                        when (cluster.defaultStorageType) {
                            SparkSubmitStorageType.ADLS_GEN1 -> model.adlsRootPath = pathUri.toString()
                            SparkSubmitStorageType.BLOB -> model.storageAccount = pathUri.host.let { it.substring(0, it.indexOf(".")) }
                            SparkSubmitStorageType.ADLS_GEN2 -> model.gen2RootPath = "https://${pathUri.host}/${pathUri.userInfo}/"
                            else -> { }
                        }
                    }
                }
            }
        }

        private fun validateStorageInfo(checkEvent:StorageCheckEvent): Observable<SparkSubmitJobUploadStorageModel> {
            val cluster = clusterSelectedSubject.value ?: return empty()

            return just(SparkSubmitJobUploadStorageModel()
                    .apply {
                        gen2Account = cluster.storageAccount?.name.takeIf {
                            cluster.storageAccount?.accountType == StorageAccountType.ADLSGen2
                        }
                    })
                    .doOnNext { model -> run {
                        getData(model)
                        setDefaultStorageType(checkEvent, cluster, model)
                        setUploadInfoForLinkedCluster(cluster, model)
                    }}
                    // set error message to prevent user from applying the changes when validation is not completed
                    .map { model -> model.apply {
                            errorMsg = "validating storage info is not completed"
                        }
                    }
                    .doOnNext { model -> setData(model) }
                    .observeOn(Schedulers.io())
                    .map { model ->
                        when (model.storageAccountType) {
                            SparkSubmitStorageType.SPARK_INTERACTIVE_SESSION -> model.apply {
                                errorMsg = null
                                uploadPath = "/SparkSubmission/"
                            }
                            SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT -> model.apply {
                                try {
                                    cluster.getConfigurationInfo()
                                    val defaultStorageAccount = cluster.storageAccount
                                    if (defaultStorageAccount == null) {
                                        errorMsg = "Cluster have no storage account"
                                        uploadPath = invalidUploadPath
                                    } else {
                                        val path = control.getUploadPath(defaultStorageAccount)
                                        if (path == null) {
                                            errorMsg = "Error getting upload path from storage account"
                                            uploadPath = invalidUploadPath
                                        } else {
                                            errorMsg = null
                                            uploadPath = path
                                        }
                                    }
                                } catch (ex: Exception) {
                                    errorMsg = "Error getting cluster storage configuration"
                                    uploadPath = invalidUploadPath
                                    log().warn(errorMsg + ". " + ExceptionUtils.getStackTrace(ex))
                                }
                            }
                            SparkSubmitStorageType.BLOB -> model.apply {
                                if (containersModel.size == 0 || containersModel.selectedItem == null) {
                                    uploadPath = invalidUploadPath
                                    errorMsg = "Azure Blob storage form is not completed"
                                } else {
                                    uploadPath = control.getAzureBlobStoragePath(
                                            ClusterManagerEx.getInstance().getBlobFullName(storageAccount),
                                            containersModel.selectedItem as String,
                                            HDStorageAccount.DefaultScheme
                                    )
                                    errorMsg = null
                                }
                            }
                            SparkSubmitStorageType.ADLS_GEN1 -> model.apply {
                                if (!AzureSparkClusterManager.getInstance().isSignedIn) {
                                    uploadPath = invalidUploadPath
                                    errorMsg = "ADLS Gen 1 storage type requires user to sign in first"
                                } else {
                                    // basic validation for ADLS root path
                                    // pattern for adl root path. e.g. adl://john.azuredatalakestore.net/root/path/
                                    if (adlsRootPath != null && !StoragePathInfo.AdlsPathPattern.toRegex().matches(adlsRootPath!!)) {
                                        uploadPath = invalidUploadPath
                                        errorMsg = "ADLS Root Path is invalid"
                                    } else {
                                        val formatAdlsRootPath = if (adlsRootPath?.endsWith("/") == true) adlsRootPath else "$adlsRootPath/"
                                        uploadPath = "${formatAdlsRootPath}SparkSubmission/"
                                        errorMsg = null
                                    }
                                }
                            }
                            SparkSubmitStorageType.ADLS_GEN2 -> model.apply {
//                                if(StringUtils.isBlank(accessKey)){
//                                    uploadPath = invalidUploadPath
//                                    errorMsg = "Accesskey for ADLS GEN2 Root Path is invalid"
//                                }else
                                if (gen2RootPath != null && !SparkBatchJob.AdlsGen2RestfulPathPattern.toRegex().matches(gen2RootPath!!)) {
                                    uploadPath = invalidUploadPath
                                    errorMsg = "ADLS GEN2 Root Path is invalid"
                                } else {
                                    val formatAdlsRootPath = if (gen2RootPath?.endsWith("/") == true) gen2RootPath else "$gen2RootPath/"
                                    uploadPath = "${formatAdlsRootPath}SparkSubmission/"
                                    errorMsg = null
                                }
                            }
                            SparkSubmitStorageType.WEBHDFS -> model.apply {
                                //pattern for webhdfs root path.e.g http://host/webhdfs/v1/
                                val rootPath = webHdfsRootPath?.trim() ?: return@apply
                                if (!SparkBatchJob.WebHDFSPathPattern.toRegex().matches(rootPath)) {
                                    uploadPath = invalidUploadPath
                                    errorMsg = "Webhdfs root path is not valid"
                                } else {
                                    val formatWebHdfsRootPath = if (rootPath.endsWith("/"))
                                        rootPath.trimEnd('/')
                                    else rootPath

                                    uploadPath = "$formatWebHdfsRootPath/SparkSubmission/"

                                    webHdfsAuthUser = when (cluster) {
                                        is SqlBigDataLivyLinkClusterDetail -> cluster.httpUserName
                                        else -> SparkSubmissionJobUploadWebHdfsSignOutCard.defaultAuthUser
                                    }

                                    errorMsg = null
                                }
                            }
                            SparkSubmitStorageType.ADLA_ACCOUNT_DEFAULT_STORAGE -> model.apply {
                                val account = cluster as? AzureSparkServerlessAccount
                                if (account != null) {
                                    uploadPath = "${account.storageRootPath}SparkSubmission/"
                                    errorMsg = null
                                } else {
                                    uploadPath = invalidUploadPath
                                    errorMsg = "Selected ADLA account does not exist"
                                }
                            }
                            SparkSubmitStorageType.NOT_SUPPORT_STORAGE_TYPE -> model.apply {
                                uploadPath = unsupportAccountType
                                errorMsg = "Storage type is not supported"
                            }
                            else -> model.apply {
                                uploadPath = invalidUploadPath
                                errorMsg = "Storage type is undefined"
                            }
                        }
                    }
                    .doOnNext { data ->
                        if (data.errorMsg != null) {
                            log().info("After selecting storage type, the storage info validation error is got: " + data.errorMsg)
                        }
                        setData(data)
                    }
        }
    }

    val viewModel = ViewModel().apply {
        Disposer.register(this@SparkSubmissionJobUploadStorageWithUploadPathPanel, this@apply)
    }

    override fun getData(data: SparkSubmitJobUploadStorageModel) {
        // Component -> Data
        data.errorMsg = storagePanel.errorMessage
        data.uploadPath = uploadPathField.text
        data.storageAccountType = viewModel.uploadStorage.deployStorageTypesModel.selectedItem as? SparkSubmitStorageType

        when (viewModel.uploadStorage.deployStorageTypeSelection) {
            SparkSubmitStorageType.BLOB -> {
                data.storageAccount = storagePanel.azureBlobCard.storageAccountField.text.trim()
                data.storageKey = storagePanel.azureBlobCard.storageKeyField.text.trim()
                data.containersModel = storagePanel.azureBlobCard.storageContainerUI.comboBox.model as DefaultComboBoxModel<String>
                data.selectedContainer = storagePanel.azureBlobCard.storageContainerUI.comboBox.selectedItem as? String
            }
            SparkSubmitStorageType.ADLS_GEN1 -> {
                data.adlsRootPath = storagePanel.adlsCard.adlsRootPathField.text.trim()
                data.subscriptionsModel = storagePanel.adlsCard.subscriptionsComboBox.comboBox.model as DefaultComboBoxModel<String>
                data.selectedSubscription = storagePanel.adlsCard.subscriptionsComboBox.comboBox.selectedItem as? String
            }
            SparkSubmitStorageType.WEBHDFS -> {
                data.webHdfsRootPath= storagePanel.webHdfsCard.webHdfsRootPathField.text.trim()
            }
            SparkSubmitStorageType.ADLS_GEN2 -> {
                data.gen2RootPath = storagePanel.adlsGen2Card.gen2RootPathField.text.trim()
                data.gen2Account = if (!StringUtils.isEmpty(data.gen2RootPath))
                    getAccount(SparkBatchJob.AdlsGen2RestfulPathPattern, data.gen2RootPath) else ""
                data.accessKey = storagePanel.adlsGen2Card.storageKeyField.text.trim()
            }
            else -> {}
        }
    }

    override fun setData(data: SparkSubmitJobUploadStorageModel) {
        if (data.storageAccountType != null && viewModel.uploadStorage.deployStorageTypesModel.selectedItem != data.storageAccountType) {
            viewModel.uploadStorage.deployStorageTypesModel.selectedItem = data.storageAccountType
        }

        // data -> Component
        val applyData: () -> Unit = {
            if (viewModel.uploadStorage.deployStorageTypeSelection != data.storageAccountType) {
                viewModel.uploadStorage.deployStorageTypeSelection = data.storageAccountType
            }

            storagePanel.errorMessage = data.errorMsg
            uploadPathField.text = data.uploadPath
            when (data.storageAccountType) {
                SparkSubmitStorageType.BLOB -> {
                    storagePanel.azureBlobCard.storageAccountField.text = data.storageAccount
                    val credentialAccount = data.getCredentialAccount(data.storageAccount, SparkSubmitStorageType.BLOB)
                    storagePanel.azureBlobCard.storageKeyField.text =
                            if (StringUtils.isEmpty(data.errorMsg) && StringUtils.isEmpty(data.storageKey)) {
                                credentialAccount?.let { secureStore?.loadPassword(credentialAccount, data.storageAccount) }
                            } else {
                                data.storageKey
                            }
                    if (data.containersModel.size == 0 && StringUtils.isEmpty(storagePanel.errorMessage) && StringUtils.isNotEmpty(data.selectedContainer)) {
                        storagePanel.azureBlobCard.storageContainerUI.comboBox.model = DefaultComboBoxModel(arrayOf(data.selectedContainer))
                    } else {
                        storagePanel.azureBlobCard.storageContainerUI.comboBox.model = data.containersModel as DefaultComboBoxModel<Any>
                    }
                }
                SparkSubmitStorageType.ADLS_GEN1 -> {
                    // Only set for changed
                    if (storagePanel.adlsCard.adlsRootPathField.text != data.adlsRootPath) {
                        storagePanel.adlsCard.adlsRootPathField.text = data.adlsRootPath
                    }

                    // show sign in/out panel based on whether user has signed in or not
                    val curLayout = storagePanel.adlsCard.azureAccountCards.layout as CardLayout
                    if (AzureSparkClusterManager.getInstance().isSignedIn()) {
                        curLayout.show(storagePanel.adlsCard.azureAccountCards, storagePanel.adlsCard.signOutCard.title)
                        storagePanel.adlsCard.signOutCard.azureAccountLabel.text = AzureSparkClusterManager.getInstance().getAzureAccountEmail()
                    } else {
                        curLayout.show(storagePanel.adlsCard.azureAccountCards, storagePanel.adlsCard.signInCard.title)
                    }

                    if (data.subscriptionsModel.size == 0 && StringUtils.isEmpty(storagePanel.errorMessage) && StringUtils.isNotEmpty(data.selectedSubscription)) {
                        storagePanel.adlsCard.subscriptionsComboBox.comboBox.model = DefaultComboBoxModel(arrayOf(data.selectedSubscription))
                    } else {
                        storagePanel.adlsCard.subscriptionsComboBox.comboBox.model = data.subscriptionsModel as DefaultComboBoxModel<Any>
                    }
                }
                SparkSubmitStorageType.WEBHDFS -> {
                    if (storagePanel.webHdfsCard.webHdfsRootPathField.text != data.webHdfsRootPath) {
                        storagePanel.webHdfsCard.webHdfsRootPathField.text = data.webHdfsRootPath
                    }

                    // show sign in/out panel based on whether user has signed in or not
                    val curLayout = storagePanel.webHdfsCard.authAccountForWebHdfsCards.layout as CardLayout
                    curLayout.show(storagePanel.webHdfsCard.authAccountForWebHdfsCards, storagePanel.webHdfsCard.signOutCard.title)
                    storagePanel.webHdfsCard.signOutCard.authUserNameLabel.text = data.webHdfsAuthUser
                }
                SparkSubmitStorageType.ADLS_GEN2 -> {
                    if (storagePanel.adlsGen2Card.gen2RootPathField.text != data.gen2RootPath){
                        storagePanel.adlsGen2Card.gen2RootPathField.text = data.gen2RootPath
                    }

                    val credentialAccount = data.getCredentialAccount(
                            getAccount(SparkBatchJob.AdlsGen2RestfulPathPattern, data.gen2RootPath),
                            SparkSubmitStorageType.ADLS_GEN2)
                    storagePanel.adlsGen2Card.storageKeyField.text =
                            if (StringUtils.isEmpty(data.accessKey)) {
                                credentialAccount?.let { secureStore?.loadPassword(credentialAccount, data.gen2Account) ?: "" }
                            } else {
                                data.accessKey
                            }
                }
            }
        }

        try {
            ApplicationManager.getApplication().invokeAndWait(applyData, ModalityState.any())
        } catch (ignore: ProcessCanceledException) {
        }

    }

    private fun getAccount(pattern: String, rootPath: String?): String? {
        return rootPath?.let {
            SparkBatchJob.AdlsGen2RestfulPathPattern.toRegex().find(rootPath)?.groupValues?.get(2)
        }
    }

    override fun dispose() {
    }
}