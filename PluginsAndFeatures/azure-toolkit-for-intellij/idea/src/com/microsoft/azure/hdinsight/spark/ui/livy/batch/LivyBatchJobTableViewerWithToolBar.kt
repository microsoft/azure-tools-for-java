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

package com.microsoft.azure.hdinsight.spark.ui.livy.batch

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.uiDesigner.core.GridConstraints
import com.microsoft.azuretools.ijidea.utility.AzureAnAction
import com.microsoft.azuretools.telemetrywrapper.Operation
import com.microsoft.intellij.forms.dsl.panel
import java.awt.Dimension
import javax.swing.JPanel

class LivyBatchJobTableViewerWithToolBar(private val jobTableViewport : LivyBatchJobTableViewport,
                                         private val refreshActionPerformed: (anActionEvent: AnActionEvent?) -> Unit) : JPanel() {
    private val refreshAction = object: AzureAnAction(AllIcons.Actions.Refresh) {
        override fun onActionPerformed(anActionEvent: AnActionEvent, operation: Operation?): Boolean {
            refreshActionPerformed(anActionEvent)
            return true
        }
    }

    private val refreshButton: ActionButton = ActionButton(refreshAction, refreshAction.templatePresentation.clone(), "Refresh button in job view tool bar", Dimension(13, 13))

    init {
        val formBuilder = panel {
            columnTemplate {
                col {
                    anchor = GridConstraints.ANCHOR_WEST
                }
                row {
                    c(refreshButton) { indent = 0; fill = GridConstraints.FILL_NONE; vSizePolicy = GridConstraints.SIZEPOLICY_FIXED }
                }
                row {
                    c(jobTableViewport.component) { indent = 0 }
                }
            }
        }

        // We don't need to send telemetry in this panel.
        // For refresh action, the telemetry is already sent in class CosmosSparkClusterOpsCtrl
        layout = formBuilder.createGridLayoutManager()
        formBuilder.allComponentConstraints.forEach { (component, gridConstrains) -> add(component, gridConstrains) }
    }
}