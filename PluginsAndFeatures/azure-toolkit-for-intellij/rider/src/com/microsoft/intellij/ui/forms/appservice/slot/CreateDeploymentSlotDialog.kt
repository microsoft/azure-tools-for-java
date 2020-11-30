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

package com.microsoft.intellij.ui.forms.appservice.slot

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.jetbrains.rd.platform.util.application
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.reactive.Signal
import com.jetbrains.rd.util.threading.SpinWait
import com.microsoft.azure.management.appservice.WebAppBase
import com.microsoft.intellij.deploy.AzureDeploymentProgressNotification
import com.microsoft.intellij.ui.extension.getSelectedValue
import com.microsoft.intellij.ui.forms.base.AzureCreateDialogBase
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle
import java.time.Duration
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel

abstract class CreateDeploymentSlotDialog(lifetimeDef: LifetimeDefinition,
                                          project: Project,
                                          private val app: WebAppBase,
                                          private val getDeploymentSlotComponent: (signal: Signal<Boolean>) -> CreateDeploymentSlotComponent,
                                          private val onCreate: () -> Unit) :
    AzureCreateDialogBase(lifetimeDef, project) {

    companion object {
        val dialogTitle: String = RiderAzureBundle.message("dialog.create_deployment_slot.title")
        val deploymentSlotCreateTimeout: Duration = Duration.ofSeconds(30)

        private const val DEPLOYMENT_SLOT_HELP_URL = "https://docs.microsoft.com/en-us/azure/app-service/deploy-staging-slots"
    }

    private val panel = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1", ":$dialogMinWidth:"))

    val activityNotifier = AzureDeploymentProgressNotification()

    val isLoadFinishedSignal = Signal<Boolean>()

    val pnlCreate: CreateDeploymentSlotComponent

    init {
        title = dialogTitle
        isOKActionEnabled = false

        isLoadFinishedSignal.advise(lifetime) { isRefreshFinished ->
            if (isRefreshFinished)
                isOKActionEnabled = true
        }

        pnlCreate = getDeploymentSlotComponent(isLoadFinishedSignal)
        myPreferredFocusedComponent = pnlCreate.pnlName.txtNameValue

        initComponentValidation()
        initMainPanel()
        init()
    }

    override val azureHelpUrl = DEPLOYMENT_SLOT_HELP_URL

    override fun createCenterPanel(): JComponent? = panel

    abstract fun createSlotAction(app: WebAppBase, slotName: String, sourceName: String?)

    abstract fun checkSlotIsCreatedAction(app: WebAppBase, slotName: String): Boolean

    override fun doOKAction() {
        val slotName = pnlCreate.slotName
        val progressMessage = RiderAzureBundle.message("dialog.create_deployment_slot.creating", slotName)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, progressMessage, true) {
            override fun run(progress: ProgressIndicator) {
                val cloneSettingsSource =
                        if (pnlCreate.isCloneSettings) pnlCreate.cbExistingSettings.getSelectedValue()
                        else null

                createSlotAction(app, slotName, cloneSettingsSource)
            }

            override fun onSuccess() {
                super.onSuccess()

                activityNotifier.notifyProgress(
                        RiderAzureBundle.message("dialog.create_deployment_slot.creating", slotName),
                        Date(),
                        null,
                        100,
                        RiderAzureBundle.message("dialog.create_deployment_slot.create_success", slotName)
                )

                SpinWait.spinUntil(lifetimeDef, deploymentSlotCreateTimeout) {
                    checkSlotIsCreatedAction(app, slotName)
                }

                application.invokeLater { onCreate() }
            }
        })

        super.doOKAction()
    }

    override fun validateComponent(): List<ValidationInfo> = pnlCreate.validateComponent()

    override fun initComponentValidation() = pnlCreate.initComponentValidation()

    private fun initMainPanel() {
        panel.add(pnlCreate, "growx")
    }
}
