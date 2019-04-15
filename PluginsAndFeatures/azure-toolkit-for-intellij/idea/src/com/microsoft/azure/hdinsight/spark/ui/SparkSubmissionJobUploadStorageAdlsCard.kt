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
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType
import com.microsoft.azuretools.ijidea.ui.HintTextField
import com.microsoft.azure.hdinsight.common.StreamUtil
import com.microsoft.intellij.forms.dsl.panel
import java.awt.CardLayout
import javax.swing.JLabel
import javax.swing.JPanel

class SparkSubmissionJobUploadStorageAdlsCard: SparkSubmissionJobUploadStorageBasicCard() {
    private val refreshButtonIconPath = "/icons/refresh.png"
    override val title: String = SparkSubmitStorageType.ADLS_GEN1.description
    private val adlsRootPathTip = "e.g. adl://myaccount.azuredatalakestore.net/<root path>"
    private val adlsRootPathLabel = JLabel("ADLS Root Path").apply { toolTipText = adlsRootPathTip }
    val adlsRootPathField = HintTextField(adlsRootPathTip)
    private val authMethodLabel = JLabel("Authentication Method")
    private val authMethodComboBox = ComboBox<String>(arrayOf("Azure Account"))
    private val subscriptionsLabel = JLabel("Subscription List")
    val subscriptionsComboBox  = ComboboxWithBrowseButton().apply {
        button.toolTipText = "Refresh"
        button.icon = StreamUtil.getImageResourceFile(refreshButtonIconPath)
    }

    val signInCard = SparkSubmissionJobUploadStorageAdlsSignInCard()
    val signOutCard = SparkSubmissionJobUploadStorageAdlsSignOutCard()
    val azureAccountCards = JPanel(CardLayout()).apply {
        add(signInCard, signInCard.title)
        add(signOutCard, signOutCard.title)
    }

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
                c(adlsRootPathLabel); c(adlsRootPathField)
            }
            row {
                c(authMethodLabel); c(authMethodComboBox)
            }
            row {
                c(); c(azureAccountCards)
            }
            row {
                c(subscriptionsLabel); c(subscriptionsComboBox)
            }
        }
        layout = formBuilder.createGridLayoutManager()
        formBuilder.allComponentConstraints.forEach { (component, gridConstrains) -> add(component, gridConstrains) }
    }
}