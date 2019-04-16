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
import com.intellij.openapi.util.Disposer
import com.intellij.ui.HideableTitledPanel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel
import com.microsoft.intellij.forms.dsl.panel
import javax.swing.JComponent

class SparkSubmissionDebuggablePanel(project: Project) : SparkSubmissionContentPanel(project) {
    private val advancedConfigPanel = SparkSubmissionAdvancedConfigPanel().apply {
        Disposer.register(this@SparkSubmissionDebuggablePanel, this@apply)
    }

    private val hidableAdvancedConfigPanel = HideableTitledPanel(
            "Advanced Configuration (Remote Debugging)", true, advancedConfigPanel, false)

    private val submissionDebuggablePanel by lazy {
        val formBuilder = panel {
            row { c(super.component) }
            row { c(hidableAdvancedConfigPanel) }
        }

        formBuilder.buildPanel()
    }

    inner class ViewModel : SparkSubmissionContentPanel.ViewModel() {
        val advancedConfig = advancedConfigPanel.viewModel

        init {
            clusterSelection.clusterIsSelected
                    .subscribe { advancedConfig.clusterSelectedSubject.onNext(it) }
        }
    }

    override val viewModel = ViewModel().apply { Disposer.register(this@SparkSubmissionDebuggablePanel, this@apply) }

    override val component: JComponent
        get() = submissionDebuggablePanel

    override fun setData(data: SparkSubmitModel) {
        // Data -> Components
        super.setData(data)

        // Advanced Configuration panel
        advancedConfigPanel.setData(data.advancedConfigModel.apply { clusterName = data.clusterName })
        hidableAdvancedConfigPanel.setOn(data.advancedConfigModel.isUIExpanded)
    }

    override fun getData(data: SparkSubmitModel) {
        // Components -> Data
        super.getData(data)

        // Advanced Configuration panel
        advancedConfigPanel.getData(data.advancedConfigModel)
        data.advancedConfigModel.clusterName = data.clusterName
    }
}