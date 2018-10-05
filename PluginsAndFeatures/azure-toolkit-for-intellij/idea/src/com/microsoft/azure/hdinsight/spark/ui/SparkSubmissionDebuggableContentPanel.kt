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

import com.microsoft.azure.hdinsight.common.CallBack
import java.awt.GridBagConstraints
import java.awt.Insets
import java.awt.event.ActionListener
import java.util.concurrent.TimeUnit
import javax.swing.JButton

class SparkSubmissionDebuggableContentPanel(updateCallBack: CallBack?) : SparkSubmissionContentPanel(updateCallBack) {

    private val advancedConfigButton = JButton("Advanced configuration")
    val advancedConfigDialog = SparkSubmissionAdvancedConfigDialog()

    init {
        addAdvancedConfigLineItem()
    }

    private fun addAdvancedConfigLineItem() {
        advancedConfigButton.isEnabled = false
        advancedConfigButton.toolTipText = "Specify advanced configuration, for example, enabling Spark remote debug"

        clusterSelectedSubject
                .throttleWithTimeout(200, TimeUnit.MILLISECONDS)
                .subscribe { cluster -> advancedConfigButton.isEnabled = cluster != null }

        add(advancedConfigButton,
                GridBagConstraints(0, ++displayLayoutCurrentRow,
                        0, 1,
                        1.0, 0.0,
                        GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE,
                        Insets(margin, margin, 0, 0), 0, 0))

    }

    fun addAdvancedConfigurationButtonActionListener(actionListener: ActionListener) {
        advancedConfigButton.addActionListener(actionListener)
    }
}