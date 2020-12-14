/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.ui.component.appservice

import com.intellij.icons.AllIcons
import com.intellij.ui.border.IdeaTitledBorder
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.azure.management.appservice.*
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.intellij.ui.component.AzureComponent
import net.miginfocom.swing.MigLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.RowFilter.regexFilter
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

class WebAppExistingComponent(lifetime: Lifetime) : AppExistingComponent<WebApp, DeploymentSlot>(lifetime) {
    override fun initDeploymentSlotsPanel(lifetime: Lifetime): DeploymentSlotPublishSettingsPanelBase<DeploymentSlot> =
            DeploymentSlotPublishSettingsPanel(lifetime)
}

class FunctionAppExistingComponent(lifetime: Lifetime) : AppExistingComponent<FunctionApp, FunctionDeploymentSlot>(lifetime) {
    override fun initDeploymentSlotsPanel(lifetime: Lifetime): DeploymentSlotPublishSettingsPanelBase<FunctionDeploymentSlot> =
            FunctionDeploymentSlotPublishSettingsPanel(lifetime)
}

abstract class AppExistingComponent<TApp : WebAppBase, TSlot: DeploymentSlotBase<TSlot>>(lifetime: Lifetime) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1")), AzureComponent {

    companion object {
        private val warningIcon = AllIcons.General.BalloonWarning
    }

    val pnlExistingAppTable = ExistingAppsTableComponent<TApp>()

    val pnlDeploymentSlotSettings: DeploymentSlotPublishSettingsPanelBase<TSlot>

    abstract fun initDeploymentSlotsPanel(lifetime: Lifetime): DeploymentSlotPublishSettingsPanelBase<TSlot>

    private val lblRuntimeMismatchWarning = JLabel()

    init {
        pnlDeploymentSlotSettings = initDeploymentSlotsPanel(lifetime)

        initExistingTableComponent()
        initRuntimeMismatchWarningLabel()
        initRefreshButton()

        apply {
            border = IdeaTitledBorder("Choose App", 0, JBUI.emptyInsets())

            add(pnlExistingAppTable, "growx")
            add(lblRuntimeMismatchWarning, "growx")
            add(pnlDeploymentSlotSettings, "growx")
        }

        initComponentValidation()
    }

    fun setRuntimeMismatchWarning(show: Boolean, message: String = "") {
        lblRuntimeMismatchWarning.text = message
        lblRuntimeMismatchWarning.isVisible = show
    }

    fun fillAppsTable(apps: List<ResourceEx<TApp>>, defaultAppId: String? = null) {
        pnlExistingAppTable.fillAppsTable(apps) {
            app -> app.id() == defaultAppId
        }
    }

    /**
     * Filter Web App table content based on selected project properties.
     * Here we should filter all Linux instances for selected project with full .Net Target Framework
     *
     * @param isDotNetCore a flag to check for DotNetCore compatible applications
     */
    fun filterAppTableContent(isDotNetCore: Boolean) {
        val sorter = pnlExistingAppTable.table.rowSorter as? TableRowSorter<*> ?: return

        if (isDotNetCore) {
            sorter.rowFilter = null
            return
        }

        val osColumnIndex = (pnlExistingAppTable.table.model as DefaultTableModel).findColumn(pnlExistingAppTable.appTableColumnOs)
        require(osColumnIndex >= 0) { "Column index is out of range" }
        sorter.rowFilter = regexFilter(OperatingSystem.WINDOWS.name.toLowerCase().capitalize(), osColumnIndex)
    }

    private fun initRefreshButton() {
        val action = pnlExistingAppTable.tableRefreshAction

        pnlExistingAppTable.tableRefreshAction = {
            setRuntimeMismatchWarning(false)
            pnlDeploymentSlotSettings.resetAppToSlotsCache()
            action()
        }
    }

    private fun initExistingTableComponent() {
        pnlExistingAppTable.table.selectionModel.addListSelectionListener {
            val app = pnlExistingAppTable.getTableSelectedApp() ?: return@addListSelectionListener
            pnlDeploymentSlotSettings.appProperty.set(app)

        }
    }

    private fun initRuntimeMismatchWarningLabel() {
        lblRuntimeMismatchWarning.icon = warningIcon
        setRuntimeMismatchWarning(false)
    }
}