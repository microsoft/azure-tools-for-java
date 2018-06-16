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

import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.common.CallBack
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel
import java.awt.event.ActionListener

class SparkSubmissionDebuggablePanelConfigurable(project: Project,
                                                 callBack: CallBack?,
                                                 submissionPanel: SparkSubmissionDebuggableContentPanel)
    : SparkSubmissionContentPanelConfigurable(project, callBack, submissionPanel) {
    private val submissionDebuggablePanel
        get() = submissionPanel as SparkSubmissionDebuggableContentPanel

    override fun createUIComponents() {
        super.createUIComponents()

        val advConfDialog = this.submissionDebuggablePanel.advancedConfigDialog
        advConfDialog.addCallbackOnOk {
            advConfDialog.getData(submitModel.advancedConfigModel)
        }

        this.submissionDebuggablePanel.addAdvancedConfigurationButtonActionListener(ActionListener {
            // Read the current panel setting into current model

            advConfDialog.setAuthenticationAutoVerify(submitModel.selectedClusterDetail
                    .map(IClusterDetail::getName)
                    .orElse(null))
            advConfDialog.isModal = true
            advConfDialog.isVisible = true
        })
    }

    override fun setData(data: SparkSubmitModel) {
        super.setData(data)

        // Advanced Configuration Dialog
        submissionDebuggablePanel.advancedConfigDialog.setData(data.advancedConfigModel)
    }

    override fun getData(data: SparkSubmitModel) {
        super.getData(data)

        // Advanced Configuration Dialog
        submissionDebuggablePanel.advancedConfigDialog.getData(data.advancedConfigModel)
    }
}