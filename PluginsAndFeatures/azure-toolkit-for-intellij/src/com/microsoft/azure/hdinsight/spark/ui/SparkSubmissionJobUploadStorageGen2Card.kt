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

import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridConstraints.*
import com.microsoft.azure.hdinsight.common.StreamUtil
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType
import com.microsoft.intellij.forms.dsl.panel
import javax.swing.JLabel
import javax.swing.JTextArea
import javax.swing.JTextField

class SparkSubmissionJobUploadStorageGen2Card : SparkSubmissionJobUploadStorageBasicCard() {
    private val refreshButtonIconPath = "/icons/refresh.png"
    private val storageKeyTip = "The access key of the default storage account, which can be found from HDInsight cluster storage accounts of Azure portal."
    private val storageKeyLabel = JLabel("Access Key").apply { toolTipText = storageKeyTip }
    val storageKeyField = ExpandableTextField().apply { toolTipText = storageKeyTip }
    private val gen2RootPathTip = "e.g. https://<mystorageaccount>.dfs.core.windows.net/<root path>."
    private val gen2RootPathLabel = JLabel("ADLS GEN2 Root Path").apply { toolTipText = gen2RootPathTip }
    val gen2RootPathField = JTextArea().apply { toolTipText = gen2RootPathTip }

    init {
        val formBuilder = panel {
            columnTemplate {
                col {
                    anchor = ANCHOR_WEST
                }
                col {
                    anchor = ANCHOR_WEST
                    hSizePolicy = GridConstraints.SIZEPOLICY_WANT_GROW
                    fill = GridConstraints.FILL_HORIZONTAL
                }
            }
            row {
                c(gen2RootPathLabel); c(gen2RootPathField)
            }
            row {
                c(storageKeyLabel); c(storageKeyField)
            }
        }

        layout = formBuilder.createGridLayoutManager()
        formBuilder.allComponentConstraints.forEach { (component, gridConstrains) -> add(component, gridConstrains) }
    }

    override val title = SparkSubmitStorageType.ADLS_GEN2.description
}