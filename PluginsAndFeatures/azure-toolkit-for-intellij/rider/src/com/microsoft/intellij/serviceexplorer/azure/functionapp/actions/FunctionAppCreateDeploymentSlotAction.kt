/**
 * Copyright (c) 2020-2021 JetBrains s.r.o.
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

package com.microsoft.intellij.serviceexplorer.azure.functionapp.actions

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.defineNestedLifetime
import com.microsoft.intellij.serviceexplorer.azure.database.actions.runWhenSignedIn
import com.microsoft.intellij.ui.forms.appservice.functionapp.slot.FunctionAppCreateDeploymentSlotDialog
import com.microsoft.tooling.msservices.helpers.Name
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionAppNode
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.deploymentslot.FunctionDeploymentSlotModule

@Name("New Deployment Slot")
class FunctionAppCreateDeploymentSlotAction(private val node: FunctionDeploymentSlotModule) : NodeActionListener() {

    companion object {
        private val logger = Logger.getInstance(FunctionAppCreateDeploymentSlotAction::class.java)
    }

    override fun actionPerformed(event: NodeActionEvent?) {

        val project = node.project as? Project
        if (project == null) {
            logger.error("Project instance is not defined for module '${node.name}'")
            return
        }

        runWhenSignedIn(project) {
            val functionAppNode = node.parent as? FunctionAppNode
            if (functionAppNode == null) {
                logger.error("Cannot find Web App node for Deployment Slot module '${node.name}'")
                return@runWhenSignedIn
            }

            val createSlotForm = FunctionAppCreateDeploymentSlotDialog(
                    lifetimeDef = project.defineNestedLifetime(),
                    project = project,
                    app = functionAppNode.functionApp,
                    onCreate = { node.load(true) })

            createSlotForm.show()
        }
    }

    override fun getAction(): AzureActionEnum = AzureActionEnum.CREATE
}
