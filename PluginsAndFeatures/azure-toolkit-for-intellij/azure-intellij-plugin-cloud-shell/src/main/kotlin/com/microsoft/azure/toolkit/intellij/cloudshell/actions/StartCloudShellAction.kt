/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.cloudshell.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.microsoft.azure.toolkit.intellij.cloudshell.CloudShellProvisioningService
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import kotlinx.coroutines.launch


class StartCloudShellAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val selectedSubscriptions = Azure.az(AzureAccount::class.java).account().selectedSubscriptions
        if (selectedSubscriptions.size == 0) return

        if (selectedSubscriptions.size == 1) {
            val subscription = selectedSubscriptions.first()
            provisionForSubscription(subscription, project)
        } else {
            val step = object :
                BaseListPopupStep<Subscription>("Select Subscription To Run Azure Cloud Shell", selectedSubscriptions) {

                override fun getTextFor(value: Subscription?) = if (value != null) "${value.name} (${value.id})" else ""

                override fun onChosen(selectedValue: Subscription, finalChoice: Boolean): PopupStep<*>? {
                    doFinalStep {
                        provisionForSubscription(selectedValue, project)
                    }
                    return PopupStep.FINAL_CHOICE
                }
            }

            val popup = JBPopupFactory.getInstance().createListPopup(step)
            popup.showCenteredInCurrentWindow(project)
        }
    }

    private fun provisionForSubscription(subscription: Subscription, project: Project) =
        currentThreadCoroutineScope().launch {
            val provisioningService = CloudShellProvisioningService.getInstance(project)
            provisioningService.provision(subscription)
        }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }

        e.presentation.isEnabled = Azure.az(AzureAccount::class.java).isLoggedIn
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}