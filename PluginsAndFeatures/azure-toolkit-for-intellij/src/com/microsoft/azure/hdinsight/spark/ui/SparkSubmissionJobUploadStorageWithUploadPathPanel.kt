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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.ui.HideableTitledPanel
import com.intellij.uiDesigner.core.GridConstraints.*
import com.microsoft.azure.hdinsight.common.ClusterManagerEx
import com.microsoft.azure.hdinsight.common.mvc.SettableControl
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.sdk.common.AzureSparkClusterManager
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitJobUploadStorageModel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType
import com.microsoft.azuretools.securestore.SecureStore
import com.microsoft.azuretools.service.ServiceManager
import com.microsoft.intellij.forms.dsl.panel
import org.apache.commons.lang3.StringUtils
import rx.subjects.PublishSubject
import java.awt.CardLayout
import javax.swing.*

open class SparkSubmissionJobUploadStorageWithUploadPathPanel : JPanel(), SettableControl<SparkSubmitJobUploadStorageModel> {
    val secureStore: SecureStore? = ServiceManager.getServiceProvider(SecureStore::class.java)
    val jobUploadStorageTitle = "Job Upload Storage"
    private val uploadPathLabel = JLabel("Upload Path")
    private val uploadPathField = JTextField().apply {
        isEditable = false
        border = BorderFactory.createEmptyBorder()
    }

    val storagePanel = createStoragePanel()
    val hideableJobUploadStoragePanel = createHideableJobUploadStoragePanel()

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

    open fun createStoragePanel() = SparkSubmissionJobUploadStoragePanel()
    open fun createHideableJobUploadStoragePanel() = HideableTitledPanel(jobUploadStorageTitle, true, storagePanel, false)

    val storageCheckSubject: PublishSubject<SparkSubmissionJobUploadStorageCtrl.StorageCheckEvent> = PublishSubject.create()

    override fun removeNotify() {
        super.removeNotify()

        storageCheckSubject.onCompleted()
    }

    override fun getData(data: SparkSubmitJobUploadStorageModel) {
        // Component -> Data
        data.errorMsg = storagePanel.errorMessage
        data.uploadPath = uploadPathField.text
        when (storagePanel.storageTypeComboBox.selectedItem) {
            storagePanel.azureBlobCard.title -> {
                data.storageAccountType = SparkSubmitStorageType.BLOB
                data.storageAccount = storagePanel.azureBlobCard.storageAccountField.text.trim()
                data.storageKey = storagePanel.azureBlobCard.storageKeyField.text.trim()
                data.containersModel = storagePanel.azureBlobCard.storageContainerUI.comboBox.model as DefaultComboBoxModel<String>
                data.selectedContainer = storagePanel.azureBlobCard.storageContainerUI.comboBox.selectedItem as? String
            }
            storagePanel.clusterDefaultStorageCard.title -> {
                data.storageAccountType = SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT
            }
            storagePanel.sparkInteractiveSessionCard.title -> {
                data.storageAccountType = SparkSubmitStorageType.SPARK_INTERACTIVE_SESSION
            }
            storagePanel.adlsCard.title -> {
                data.storageAccountType = SparkSubmitStorageType.ADLS_GEN1
                data.adlsRootPath = storagePanel.adlsCard.adlsRootPathField.text
            }
            storagePanel.webHdfsCard.title -> {
                data.storageAccountType = SparkSubmitStorageType.WEBHDFS
                data.webHdfsRootPath= storagePanel.webHdfsCard.webHdfsRootPathField.text
            }
        }
    }

    override fun setData(data: SparkSubmitJobUploadStorageModel) {
        // data -> Component
        val applyData: () -> Unit = {
            // Only set for changed
            if (storagePanel.storageTypeComboBox.selectedIndex != findStorageTypeComboBoxSelectedIndex(data.storageAccountType)) {
                storagePanel.storageTypeComboBox.selectedIndex = findStorageTypeComboBoxSelectedIndex(data.storageAccountType)
            }

            storagePanel.errorMessage = data.errorMsg
            uploadPathField.text = data.uploadPath
            when (data.storageAccountType) {
                SparkSubmitStorageType.BLOB -> {
                    storagePanel.azureBlobCard.storageAccountField.text = data.storageAccount
                    val credentialAccount = data.getCredentialAzureBlobAccount()
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
                }
                SparkSubmitStorageType.WEBHDFS -> {
                    if(storagePanel.webHdfsCard.webHdfsRootPathField.text != data.webHdfsRootPath){
                        storagePanel.webHdfsCard.webHdfsRootPathField.text = data.webHdfsRootPath
                    }

                    // show sign in/out panel based on whether user has signed in or not
                    val curLayout = storagePanel.webHdfsCard.authAccountForWebHdfsCards.layout as CardLayout
                    curLayout.show(storagePanel.webHdfsCard.authAccountForWebHdfsCards,storagePanel.webHdfsCard.signOutCard.title)
                    storagePanel.webHdfsCard.signOutCard.authUserNameLabel.text = data.webHdfsAuthUser
                }
            }
        }
        ApplicationManager.getApplication().invokeLater(applyData, ModalityState.any())
    }

    private fun findStorageTypeComboBoxSelectedIndex(storageAccountType: SparkSubmitStorageType):Int {
        listOf(0 until storagePanel.storageTypeComboBox.model.size).flatten().forEach {
            if ((storagePanel.storageTypeComboBox.model.getElementAt(it) == storagePanel.azureBlobCard.title && storageAccountType == SparkSubmitStorageType.BLOB) ||
                    (storagePanel.storageTypeComboBox.model.getElementAt(it) == storagePanel.sparkInteractiveSessionCard.title && storageAccountType == SparkSubmitStorageType.SPARK_INTERACTIVE_SESSION) ||
                    (storagePanel.storageTypeComboBox.model.getElementAt(it) == storagePanel.clusterDefaultStorageCard.title && storageAccountType == SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT) ||
                    (storagePanel.storageTypeComboBox.model.getElementAt(it) == storagePanel.adlsCard.title && storageAccountType == SparkSubmitStorageType.ADLS_GEN1) ||
                    (storagePanel.storageTypeComboBox.model.getElementAt(it) == storagePanel.webHdfsCard.title && storageAccountType == SparkSubmitStorageType.WEBHDFS)) {
                return it
            }
        }
        return -1
    }
}