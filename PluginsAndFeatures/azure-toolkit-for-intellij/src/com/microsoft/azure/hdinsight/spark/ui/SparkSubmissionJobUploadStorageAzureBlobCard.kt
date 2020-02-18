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

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.uiDesigner.core.GridConstraints.*
import com.microsoft.azure.hdinsight.common.ClusterManagerEx
import com.microsoft.azure.hdinsight.common.StreamUtil
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount.DefaultScheme
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitJobUploadStorageModel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType.BLOB
import com.microsoft.azure.hdinsight.spark.common.getSecureStoreServiceOf
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionContentPanel.Constants.Companion.submissionFolder
import com.microsoft.azure.storage.blob.BlobRequestOptions
import com.microsoft.azuretools.securestore.SecureStore
import com.microsoft.azuretools.service.ServiceManager
import com.microsoft.intellij.forms.dsl.panel
import com.microsoft.intellij.ui.util.UIUtils
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager
import com.microsoft.tooling.msservices.model.storage.BlobContainer
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import rx.Observable
import rx.schedulers.Schedulers
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.event.DocumentEvent

class SparkSubmissionJobUploadStorageAzureBlobCard
    : SparkSubmissionJobUploadStorageBasicCard(BLOB.description), ILogger {
    interface Model : SparkSubmissionJobUploadStorageBasicCard.Model {
        var storageAccount : String?
        var storageKey : String?
        var containersModel : ComboBoxModel<Any>
        var selectedContainer: String?
    }

    private val secureStore: SecureStore? = ServiceManager.getServiceProvider(SecureStore::class.java)
    private val refreshButtonIconPath = "/icons/refresh.png"
    private val storageAccountTip = "The default storage account of the HDInsight cluster, which can be found from HDInsight cluster properties of Azure portal."
    private val storageKeyTip = "The storage key of the default storage account, which can be found from HDInsight cluster storage accounts of Azure portal."
    private val storageAccountLabel = JLabel("Storage Account").apply { toolTipText = storageAccountTip }
    private val storageAccountField = JTextField().apply {
        toolTipText = storageAccountTip
        name = "blobCardStorageAccountField"

        // Each time user changed storage account or key, we set the containers to empty
        document.addDocumentListener(textChangeTriggerResetStorageContainerListener)
        addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) = doRefresh()
        })
    }

    private val storageKeyLabel = JLabel("Storage Key").apply { toolTipText = storageKeyTip }
    private val storageKeyField = ExpandableTextField().apply {
        toolTipText = storageKeyTip
        name = "blobCardStorageKeyField"

        // Each time user changed storage account or key, we set the containers to empty
        document.addDocumentListener(textChangeTriggerResetStorageContainerListener)
        addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) = doRefresh()
        })
    }

    private val storageContainerLabel = JLabel("Storage Container")
    private val storageContainerUI = ComboboxWithBrowseButton().apply {
        comboBox.name = "blobCardStorageContainerComboBoxCombo"

        // after container is selected or new model is set, update upload path
        comboBox.addPropertyChangeListener("model") {
            if ((it.oldValue as? ComboBoxModel<*>)?.selectedItem != (it.newValue as? ComboBoxModel<*>)?.selectedItem) {
                updateStorageAfterContainerSelected().subscribe(
                        { },
                        { err -> log().warn(ExceptionUtils.getStackTrace(err)) })
            }
        }

        comboBox.addItemListener { itemEvent ->
            if (itemEvent?.stateChange == ItemEvent.SELECTED) {
                updateStorageAfterContainerSelected().subscribe(
                        { },
                        { err -> log().warn(ExceptionUtils.getStackTrace(err)) })
            }
        }

        button.name = "blobCardStorageContainerComboBoxButton"
        button.toolTipText = "Refresh"
        button.icon = StreamUtil.getImageResourceFile(refreshButtonIconPath)
        button.addActionListener { doRefresh() }
    }

    private val textChangeTriggerResetStorageContainerListener = object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) {
            storageContainerUI.comboBox.model = DefaultComboBoxModel()
        }
    }

    @Synchronized
    private fun doRefresh() {
        if (storageContainerUI.button.isEnabled) {
            storageContainerUI.button.isEnabled = false
            refreshContainers()
                    .doOnEach { storageContainerUI.button.isEnabled = true }
                    .subscribe(
                            { },
                            { err -> log().warn(ExceptionUtils.getStackTrace(err)) })
        }
    }

    override val view: JComponent by lazy {
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
                c(storageAccountLabel.apply { labelFor = storageAccountField });    c(storageAccountField)
            }
            row {
                c(storageKeyLabel.apply { labelFor = storageKeyField });            c(storageKeyField)
            }
            row {
                c(storageContainerLabel.apply { labelFor = storageContainerUI });   c(storageContainerUI)
            }
        }

        formBuilder.buildPanel()
    }

    private fun refreshContainers(): Observable<SparkSubmitJobUploadStorageModel> {
        ApplicationManager.getApplication().assertIsDispatchThread()

        return Observable.just(SparkSubmitJobUploadStorageModel())
                .doOnNext { getData(it) }
                // set error message to prevent user from applying the change when refreshing is not completed
                .map { it.apply { errorMsg = "refreshing storage containers is not completed" } }
                .doOnNext { setData(it) }
                .observeOn(Schedulers.io())
                .map { toUpdate ->
                    toUpdate.apply {
                        if (StringUtils.isEmpty(toUpdate.storageAccount) || StringUtils.isEmpty(toUpdate.storageKey)) {
                            errorMsg = "Storage account and key can't be empty"
                        } else {
                            try {
                                val clientStorageAccount = ClientStorageAccount(toUpdate.storageAccount)
                                        .apply { primaryKey = toUpdate.storageKey }

                                // Add Timeout for list containers operation to avoid getting stuck
                                // when storage account or key is invalid
                                val requestOptions = BlobRequestOptions().apply { maximumExecutionTimeInMs = 5000 }
                                val containers = StorageClientSDKManager
                                        .getManager()
                                        .getBlobContainers(clientStorageAccount.connectionString, requestOptions)
                                        .map(BlobContainer::getName)
                                        .toTypedArray()
                                if (containers.isNotEmpty()) {
                                    containersModel = DefaultComboBoxModel(containers)
                                    containersModel.selectedItem = containersModel.getElementAt(0)
                                    selectedContainer = containersModel.getElementAt(0)?.toString()

                                    val blobName = ClusterManagerEx.getInstance().getBlobFullName(storageAccount)
                                    uploadPath = buildBlobUri(blobName, selectedContainer)
                                    errorMsg = null
                                } else {
                                    errorMsg = "No container found in this storage account"
                                }
                            } catch (ex: Exception) {
                                log().info("Refresh Azure Blob contains error. " + ExceptionUtils.getStackTrace(ex))
                                errorMsg = "Can't get storage containers, check if account and key matches"
                            }
                        }
                    }
                }
                .doOnNext { data ->
                    if (data.errorMsg != null) {
                        log().info("Refresh Azure Blob containers error: " + data.errorMsg)
                    }
                    setData(data)
                }
    }

    private fun updateStorageAfterContainerSelected(): Observable<SparkSubmitJobUploadStorageModel> {
        ApplicationManager.getApplication().assertIsDispatchThread()

        return Observable.just(SparkSubmitJobUploadStorageModel())
                .doOnNext { getData(it) }
                // set error message to prevent user from applying the change when updating is not completed
                .map { it.apply { errorMsg = "updating upload path is not completed" } }
                .doOnNext { setData(it) }
                .observeOn(Schedulers.io())
                .map { toUpdate ->
                    if (toUpdate.containersModel.size == 0) {
                        toUpdate.apply { errorMsg = "Storage account has no containers" }
                    } else {
                        toUpdate.apply {
                            val selectedContainer = toUpdate.containersModel.selectedItem as String
                            uploadPath = buildBlobUri(
                                    ClusterManagerEx.getInstance().getBlobFullName(storageAccount),
                                    selectedContainer)
                            errorMsg = null
                        }
                    }
                }
                .doOnNext { data ->
                    if (data.errorMsg != null) {
                        log().info("Update storage info after container selected error: " + data.errorMsg)
                    }
                    setData(data)
                }
    }

    private fun buildBlobUri(fullStorageBlobName: String?, container: String?): String {
        if (StringUtils.isBlank(fullStorageBlobName) || StringUtils.isBlank(container))
            throw IllegalArgumentException("Blob Name ,container and scheme name cannot be empty")

        return "$DefaultScheme://$container@$fullStorageBlobName/$submissionFolder/"
    }

    override fun createViewModel(): ViewModel = object : ViewModel() {
        override fun getValidatedStorageUploadPath(config: SparkSubmissionJobUploadStorageBasicCard.Model)
                : String {
            if (config !is Model) {
                return INVALID_UPLOAD_PATH;
            }

            // There are IO operations
            UIUtils.assertInPooledThread()

            if (config.containersModel.size == 0
                    || config.containersModel.selectedItem == null
                    || config.storageAccount.isNullOrBlank()
                    || config.storageKey.isNullOrBlank()) {
                throw RuntimeConfigurationError("Azure Blob storage form is not completed")
            }

            return buildBlobUri(
                    ClusterManagerEx.getInstance().getBlobFullName(config.storageAccount),
                    config.containersModel.selectedItem as String)
        }
    }

    override fun readWithLock(to: SparkSubmissionJobUploadStorageBasicCard.Model) {
        if (to !is Model) {
            return
        }

        to.storageAccount = storageAccountField.text?.trim()
        to.storageKey = storageKeyField.text?.trim()
        to.containersModel = storageContainerUI.comboBox.model
        to.selectedContainer = storageContainerUI.comboBox.selectedItem?.toString()
    }

    override fun writeWithLock(from: SparkSubmissionJobUploadStorageBasicCard.Model) {
        if (from !is Model) {
            return
        }

        if (storageAccountField.text != from.storageAccount) {
            storageAccountField.text = from.storageAccount
        }

        val credentialAccount = BLOB.getSecureStoreServiceOf(from.storageAccount)
        val storageKeyToSet =
                if (StringUtils.isBlank(from.errorMsg) && StringUtils.isEmpty(from.storageKey)) {
                    credentialAccount?.let { secureStore?.loadPassword(credentialAccount, from.storageAccount) }
                } else {
                    from.storageKey
                }

        if (storageKeyField.text != storageKeyToSet) {
            storageKeyField.text = storageKeyToSet
        }

        if (storageContainerUI.comboBox.model != from.containersModel) {
            if (from.containersModel.size == 0
                    && StringUtils.isNotEmpty(from.selectedContainer)) {
                storageContainerUI.comboBox.model = DefaultComboBoxModel(arrayOf(from.selectedContainer))
            } else {
                storageContainerUI.comboBox.model = from.containersModel
            }
        }
    }
}