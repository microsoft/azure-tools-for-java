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

import com.intellij.openapi.ui.ComboBox
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import javax.swing.JLabel
import javax.swing.JTextArea
import javax.swing.JTextField

class SparkSubmissionJobUploadStorageAzureBlobCard: SparkSubmissionJobUploadStorageBasicCard() {
    private fun baseConstraints() = GridConstraints().apply { anchor = GridConstraints.ANCHOR_WEST }
    private val colTemplate= listOf(
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
                fill = GridConstraints.FILL_HORIZONTAL })
    private fun buildConstraints(colTemplateOffset: Int): GridConstraints = colTemplate[colTemplateOffset].clone() as GridConstraints

    private val storageAccountTip = "The default storage account of the HDInsight cluster, which can be found from HDInsight cluster properties of Azure portal."
    private val storageKeyTip = "The storage key of the default storage account, which can be found from HDInsight cluster storage accounts of Azure portal."
    private val storageAccountLabel = JLabel("Storage Account").apply { toolTipText = storageAccountTip }
    val storageAccountField = JTextField().apply { toolTipText = storageAccountTip }
    private val storageKeyLabel = JLabel("Storage Key").apply { toolTipText = storageKeyTip }
    val storageKeyField = JTextArea().apply { toolTipText = storageKeyTip }
    private val storageContainerLabel = JLabel("Storage Container")
    val storageContainerComboBox = ComboBox<String>()

    private val cardLayoutPlan = listOf(
            Place(storageAccountLabel, buildConstraints(0).apply { row = 0 }), Place(storageAccountField, buildConstraints(1).apply { row = 0 }),
            Place(storageKeyLabel, buildConstraints(0).apply { row = 1 }), Place(storageKeyField, buildConstraints(1).apply { row = 1 }),
            Place(storageContainerLabel, buildConstraints(0).apply { row = 2 }), Place(storageContainerComboBox, buildConstraints(1).apply { row = 2 })
    )

    init {
        layout = GridLayoutManager(cardLayoutPlan.last().gridConstraints.row + 1, colTemplate.size)
        cardLayoutPlan.forEach { (component, gridConstrains) -> add(component, gridConstrains) }
    }

    override val title = "Azure Blob"
}