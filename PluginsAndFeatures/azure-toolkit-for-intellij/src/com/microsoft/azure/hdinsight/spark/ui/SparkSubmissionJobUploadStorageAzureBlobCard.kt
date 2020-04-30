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
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.uiDesigner.core.GridConstraints.*
import com.microsoft.azure.hdinsight.common.ClusterManagerEx
import com.microsoft.azure.hdinsight.common.StreamUtil
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.common.viewmodels.ComboBoxSelectionDelegated
import com.microsoft.azure.hdinsight.common.viewmodels.ImmutableComboBoxModelDelegated
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount.DefaultScheme
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitJobUploadStorageModel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType.BLOB
import com.microsoft.azure.hdinsight.spark.common.getSecureStoreServiceOf
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionContentPanel.Constants.Companion.submissionFolder
import com.microsoft.azure.storage.blob.BlobRequestOptions
import com.microsoft.azuretools.securestore.SecureStore
import com.microsoft.azuretools.service.ServiceManager
import com.microsoft.intellij.forms.dsl.panel
import com.microsoft.intellij.rxjava.IdeaSchedulers
import com.microsoft.intellij.ui.util.UIUtils
import com.microsoft.intellij.ui.util.findFirst
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager
import com.microsoft.tooling.msservices.model.storage.BlobContainer
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.EMPTY
import org.apache.commons.lang3.exception.ExceptionUtils
import rx.Observable
import rx.schedulers.Schedulers
import java.awt.Font
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
    private val storageContainerUI = ComboboxWithBrowseButton(JComboBox(ImmutableComboBoxModel.empty<String>())).apply {
        comboBox.name = "blobCardStorageContainerComboBoxCombo"

        // after container is selected or new model is set, update upload path
        comboBox.addPropertyChangeListener("model") {
            if ((it.oldValue as? ComboBoxModel<*>)?.selectedItem != (it.newValue as? ComboBoxModel<*>)?.selectedItem) {
                viewModel.storageCheckSubject.onNext(StorageCheckEvent.InputChangedEvent(comboBox))
            }
        }

        comboBox.addItemListener { itemEvent ->
            if (itemEvent?.stateChange == ItemEvent.SELECTED) {
                viewModel.storageCheckSubject.onNext(StorageCheckEvent.InputChangedEvent(comboBox))
            }
        }

        comboBox.renderer = object : SimpleListCellRenderer<Any>() {
            override fun customize(list: JList<out Any>, value: Any?, index: Int, selected: Boolean, hasFocus: Boolean) {
                font = if (value != null) {
                    text = value.toString()
                    font.deriveFont(Font.PLAIN)
                } else {
                    text = (viewModel as ViewModel).refreshContainerError
                            ?.takeIf { it.isNotBlank() }
                            ?.let { "<$it>" }
                            ?: "<No selection>"
                    font.deriveFont(Font.ITALIC)
                }
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
            (viewModel as ViewModel).refreshContainers()
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

    inner class ViewModel: SparkSubmissionJobUploadStorageBasicCard.ViewModel() {
        var blobContainerSelection: Any? by ComboBoxSelectionDelegated(storageContainerUI.comboBox)
        var blobContainerModel: ImmutableComboBoxModel<Any> by ImmutableComboBoxModelDelegated(storageContainerUI.comboBox)
        var refreshContainerError: String? = null

        override fun getValidatedStorageUploadPath(config: SparkSubmissionJobUploadStorageBasicCard.Model)
                : String {
            if (config !is Model) {
                return INVALID_UPLOAD_PATH
            }

            // There are IO operations
            UIUtils.assertInPooledThread()

            if (config.selectedContainer.isNullOrBlank()
                    || config.storageAccount.isNullOrBlank()
                    || config.storageKey.isNullOrBlank()) {
                throw RuntimeConfigurationError("Azure Blob storage form is not completed")
            }

            return buildBlobUri(
                    ClusterManagerEx.getInstance().getBlobFullName(config.storageAccount),
                    config.selectedContainer as String)
        }

        private val ideaSchedulers = IdeaSchedulers()

        fun refreshContainers(): Observable<ImmutableComboBoxModel<Any>> {
            ApplicationManager.getApplication().assertIsDispatchThread()

            return Observable.just(SparkSubmitJobUploadStorageModel())
                    .doOnNext { getData(it) }
                    .observeOn(Schedulers.io())
                    .map { config ->
                        if (StringUtils.isEmpty(config.storageAccount) || StringUtils.isEmpty(config.storageKey)) {
                            throw RuntimeConfigurationError("Storage account and key can't be empty")
                        }

                        val clientStorageAccount = ClientStorageAccount(config.storageAccount)
                                .apply { primaryKey = config.storageKey }

                        // Add Timeout for list containers operation to avoid getting stuck
                        // when storage account or key is invalid
                        val requestOptions = BlobRequestOptions().apply { maximumExecutionTimeInMs = 5000 }
                        val containers = StorageClientSDKManager
                                .getManager()
                                .getBlobContainers(clientStorageAccount.connectionString, requestOptions)
                                .map { it.name as Any }
                                .toTypedArray()

                        if (containers.isEmpty()) {
                            throw RuntimeConfigurationError("No container found in this storage account")
                        }

                        refreshContainerError = null
                        ImmutableComboBoxModel(containers).apply {
                            findFirst { containerName -> containerName == config.selectedContainer }
                                    ?.let { found -> selectedItem = found }
                        }
                    }
                    .doOnError {
                        log().info("Refresh Azure Blob containers error: $it")

                        refreshContainerError = it.message
                        blobContainerModel = ImmutableComboBoxModel.empty()

                        storageCheckSubject.onNext(StorageCheckEvent.InputFocusLostEvent(storageContainerUI.comboBox))
                    }
                    .observeOn(ideaSchedulers.dispatchUIThread())
                    .doOnNext { containersComboModel -> blobContainerModel = containersComboModel }
        }

        private fun buildBlobUri(fullStorageBlobName: String?, container: String?): String {
            if (StringUtils.isBlank(fullStorageBlobName) || StringUtils.isBlank(container))
                throw IllegalArgumentException("Blob Name ,container and scheme name cannot be empty")

            return "$DefaultScheme://$container@$fullStorageBlobName/$submissionFolder/"
        }
    }

    override fun createViewModel(): ViewModel = ViewModel()

    override fun readWithLock(to: SparkSubmissionJobUploadStorageBasicCard.Model) {
        if (to !is Model) {
            return
        }

        to.storageAccount = storageAccountField.text?.trim()
        to.storageKey = storageKeyField.text?.trim()
        to.selectedContainer = (viewModel as ViewModel).blobContainerSelection?.toString()
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
                if (StringUtils.isBlank(viewModel.errorMessage) && StringUtils.isEmpty(from.storageKey)) {
                    credentialAccount?.let { secureStore?.loadPassword(credentialAccount, from.storageAccount) }
                } else {
                    from.storageKey
                }

        if (storageKeyField.text != storageKeyToSet) {
            storageKeyField.text = storageKeyToSet
        }

        (viewModel as ViewModel).apply {
            val found = blobContainerModel.findFirst { it == from.selectedContainer }
            if (found != null) {
                blobContainerSelection = found
            } else {
                blobContainerModel = ImmutableComboBoxModel<Any>(arrayOf(from.selectedContainer ?: EMPTY))
            }
        }
    }
}