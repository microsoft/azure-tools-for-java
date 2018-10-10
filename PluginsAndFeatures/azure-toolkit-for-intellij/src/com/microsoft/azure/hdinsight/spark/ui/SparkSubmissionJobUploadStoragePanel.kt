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
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType
import java.awt.CardLayout
import javax.swing.*

class SparkSubmissionJobUploadStoragePanel: JPanel() {
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

    val notFinishCheckMessage = "job upload storage validation check is not finished"
    private val storageTypeLabel = JLabel("Storage Type")
    val azureBlobCard = SparkSubmissionJobUploadStorageAzureBlobCard()
    val sparkInteractiveSessionCard = SparkSubmissionJobUploadStorageSparkInteractiveSessionCard()
    val clusterDefaultStorageCard = SparkSubmissionJobUploadStorageClusterDefaultStorageCard()
    val storageTypeComboBox = ComboBox(arrayOf(azureBlobCard.title, sparkInteractiveSessionCard.title, clusterDefaultStorageCard.title))
    val storageCardsPanel = JPanel(CardLayout()).apply {
        add(azureBlobCard, azureBlobCard.title)
        add(sparkInteractiveSessionCard, sparkInteractiveSessionCard.title)
        add(clusterDefaultStorageCard, clusterDefaultStorageCard.title)
    }
    var storageAccountType: SparkSubmitStorageType = SparkSubmitStorageType.BLOB
    var errorMessage: String? = notFinishCheckMessage
    private val layoutPlan = listOf(
            Place(storageTypeLabel, buildConstraints(0).apply { row = 0 }), Place(storageTypeComboBox, buildConstraints(1).apply { row = 0; indent = 3 }),
            Place(storageCardsPanel, baseConstraints().apply { row = 1; colSpan = 2; hSizePolicy = GridConstraints.SIZEPOLICY_WANT_GROW; fill = GridConstraints.FILL_HORIZONTAL })
    )

    init {
        layout = GridLayoutManager(layoutPlan.last().gridConstraints.row + 1, colTemplate.size)
        layoutPlan.forEach { (component, gridConstrains) -> add(component, gridConstrains) }
    }
}