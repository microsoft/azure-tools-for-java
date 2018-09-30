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
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.microsoft.azure.hdinsight.common.mvc.SettableControl
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitJobUploadStorageModel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType
import org.apache.commons.lang3.StringUtils
import java.awt.Component
import javax.swing.*

class SparkSubmissionJobUploadStorageWithUploadPathPanel : JPanel(), SettableControl<SparkSubmitJobUploadStorageModel> {
    data class Place(val component: Component, val gridConstraints: GridConstraints)

    private fun baseConstraints() = GridConstraints().apply { anchor = GridConstraints.ANCHOR_WEST }
    private val colTemplate = listOf(
            // Column 0
            baseConstraints().apply {
                column = 0
                indent = 1
            },
            //  Column 1
            baseConstraints().apply {
                column = 1
                indent = 1
                hSizePolicy = GridConstraints.SIZEPOLICY_WANT_GROW
                fill = GridConstraints.FILL_HORIZONTAL
            })

    private fun buildConstraints(colTemplateOffset: Int): GridConstraints = colTemplate[colTemplateOffset].clone() as GridConstraints

    private val jobUploadStorageTitle = "Job Upload Storage"
    private val uploadPathLabel = JLabel("Upload Path")
    private val uploadPathField = JTextField().apply {
        isEditable = false
        border = BorderFactory.createEmptyBorder()
    }
    val storagePanel = SparkSubmissionJobUploadStoragePanel()
    private val hideableJobUploadStoragePanel = HideableTitledPanel(jobUploadStorageTitle, true, storagePanel, false)

    private val layoutPlan = listOf(
            Place(uploadPathLabel, buildConstraints(0).apply { row = 0 }), Place(uploadPathField, buildConstraints(1).apply { row = 0 }),
            Place(hideableJobUploadStoragePanel, baseConstraints().apply {
                row = 1
                colSpan = 2
                hSizePolicy = GridConstraints.SIZEPOLICY_WANT_GROW
                fill = GridConstraints.FILL_HORIZONTAL

            })
    )

    init {
        layout = GridLayoutManager(layoutPlan.last().gridConstraints.row + 1, colTemplate.size)
        layoutPlan.forEach { (component, gridConstrains) -> add(component, gridConstrains) }
    }

    override fun getData(data: SparkSubmitJobUploadStorageModel) {
        // Component -> Data
        data.selectedStorageTypeIndex = storagePanel.storageTypeComboBox.selectedIndex
        data.errorMsg = storagePanel.errorMessage
        data.uploadPath = uploadPathField.text
        when (storagePanel.storageTypeComboBox.selectedItem) {
            StorageType.AzureBlob.title -> {
                data.storageAccountType = SparkSubmitStorageType.Blob.toString()
                data.storageAccount = storagePanel.azureBlobCard.storageAccountField.text.trim()
                data.storageKey = storagePanel.azureBlobCard.storageKeyField.text.trim()
                data.containersModel = storagePanel.azureBlobCard.storageContainerComboBox.model as DefaultComboBoxModel
                data.selectedContainer = storagePanel.azureBlobCard.storageContainerComboBox.selectedItem as? String
            }
            StorageType.ClusterDefaultStorage.title -> {
                data.storageAccountType = SparkSubmitStorageType.DefaultStorageAccount.toString()
            }
            StorageType.SparkInteractiveSession.title -> {
                data.storageAccountType = SparkSubmitStorageType.SparkInteractiveSession.toString()
            }
        }
    }

    override fun setData(data: SparkSubmitJobUploadStorageModel) {
        // data -> Component
        val applyData: () -> Unit = {
            storagePanel.storageTypeComboBox.selectedIndex = data.selectedStorageTypeIndex
            storagePanel.errorMessage = data.errorMsg
            storagePanel.storageAccountType = data.storageAccountType
            uploadPathField.text = data.uploadPath
            if (data.selectedStorageTypeIndex != -1) {
                if (storagePanel.storageTypeComboBox.getItemAt(data.selectedStorageTypeIndex) == StorageType.AzureBlob.title) {
                        storagePanel.azureBlobCard.storageAccountField.text = data.storageAccount
                        storagePanel.azureBlobCard.storageKeyField.text = data.storageKey
                        if (data.containersModel.size == 0 && StringUtils.isEmpty(storagePanel.errorMessage) && StringUtils.isNotEmpty(data.selectedContainer)) {
                            storagePanel.azureBlobCard.storageContainerComboBox.model = DefaultComboBoxModel(arrayOf(data.selectedContainer))
                        } else {
                            storagePanel.azureBlobCard.storageContainerComboBox.model = data.containersModel
                        }
                }
            }
        }
        ApplicationManager.getApplication().invokeLater(applyData, ModalityState.any())
    }


}