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
import com.intellij.uiDesigner.core.GridConstraints.*
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType
import com.microsoft.intellij.forms.dsl.panel
import java.awt.CardLayout
import javax.swing.*

class SparkSubmissionJobUploadStoragePanel: JPanel() {
    private val notFinishCheckMessage = "job upload storage validation check is not finished"
    private val storageTypeLabel = JLabel("Storage Type")
    val azureBlobCard = SparkSubmissionJobUploadStorageAzureBlobCard()
    val sparkInteractiveSessionCard = SparkSubmissionJobUploadStorageSparkInteractiveSessionCard()
    val clusterDefaultStorageCard = SparkSubmissionJobUploadStorageClusterDefaultStorageCard()
    val adlsCard = SparkSubmissionJobUploadStorageAdlsCard()
    val webHdfsCard = SparkSubmissionJobUploadStorageWebHdfsCard()
    val storageTypeComboBox = ComboBox(arrayOf(azureBlobCard.title, sparkInteractiveSessionCard.title, clusterDefaultStorageCard.title, adlsCard.title, webHdfsCard.title))
    val storageCardsPanel = JPanel(CardLayout()).apply {
        add(azureBlobCard, azureBlobCard.title)
        add(sparkInteractiveSessionCard, sparkInteractiveSessionCard.title)
        add(clusterDefaultStorageCard, clusterDefaultStorageCard.title)
        add(adlsCard, adlsCard.title)
        add(webHdfsCard, webHdfsCard.title)
    }
    var errorMessage: String? = notFinishCheckMessage

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
            }
            row {
                c(storageTypeLabel) { indent = 2 }; c(storageTypeComboBox) { indent = 3 }
            }
            row {
                c(storageCardsPanel) { indent = 2; colSpan = 2; hSizePolicy = SIZEPOLICY_WANT_GROW; fill = FILL_HORIZONTAL}
            }
        }

        layout = formBuilder.createGridLayoutManager()
        formBuilder.allComponentConstraints.forEach { (component, gridConstrains) -> add(component, gridConstrains) }
    }
}