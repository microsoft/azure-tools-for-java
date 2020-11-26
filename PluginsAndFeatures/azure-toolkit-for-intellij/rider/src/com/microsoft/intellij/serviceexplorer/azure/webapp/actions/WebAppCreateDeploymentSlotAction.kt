/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

package com.microsoft.intellij.serviceexplorer.azure.webapp.actions

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.defineNestedLifetime
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction
import com.microsoft.intellij.ui.forms.appservice.webapp.slot.WebAppCreateDeploymentSlotDialog
import com.microsoft.tooling.msservices.helpers.Name
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotModule

@Name("New Deployment Slot")
class WebAppCreateDeploymentSlotAction(private val node: DeploymentSlotModule) : NodeActionListener() {

    companion object {
        private val logger = Logger.getInstance(WebAppCreateDeploymentSlotAction::class.java)
    }

    override fun actionPerformed(event: NodeActionEvent?) {
        val project = node.project as? Project
        if (project == null) {
            logger.error("Project instance is not defined for module '${node.name}'")
            return
        }

        if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) {
            logger.error("Failed to create Deployment Slot. User is not signed in.")
            return
        }

        val webAppNode = node.parent as? WebAppNode
        if (webAppNode == null) {
            logger.error("Cannot find Web App node for Deployment Slot module '${node.name}'")
            return
        }

        val createSlotForm = WebAppCreateDeploymentSlotDialog(
                lifetimeDef = project.defineNestedLifetime(),
                project = project,
                app = webAppNode.webapp,
                onCreate = { node.load(true) })

        createSlotForm.show()
    }

    override fun getIconPath(): String = "AddEntity.svg"
}