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
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.AnActionButton
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azure.management.appservice.WebAppBase
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.extension.setComponentsVisible
import icons.CommonIcons
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.awt.Component
import java.awt.Dimension
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

open class ExistingAppsTableComponent<T : WebAppBase> :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1, hidemode 3")),
        AzureComponent {

    companion object {
        private val osWindowsIcon = CommonIcons.OS.Windows
        private val osLinuxIcon = CommonIcons.OS.Linux
    }

    val appTableColumnSubscription  = message("run_config.publish.form.existing_app.table.subscription")
    val appTableColumnName          = message("run_config.publish.form.existing_app.table.name")
    val appTableColumnResourceGroup = message("run_config.publish.form.existing_app.table.resource_group")
    val appTableColumnLocation      = message("run_config.publish.form.existing_app.table.location")
    val appTableColumnOs            = message("run_config.publish.form.existing_app.table.os")
    val appTableColumnRuntime       = message("run_config.publish.form.existing_app.table.runtime")

    private val tableLoadingMessage = message("run_config.publish.form.existing_app.table.loading")

    val table = initExistingAppsTable()
    val btnRefresh = initRefreshButton()
    var tableRefreshAction = {}
    var tableSelectAction: (T) -> Unit = {}
    private val pnlAppTable: JPanel
    private val txtSelectedApp = JTextField()

    var lastSelectedResource: ResourceEx<T>? = null
    var cachedAppList = listOf<ResourceEx<T>>()

    init {
        pnlAppTable = ToolbarDecorator
                .createDecorator(table)
                .addExtraActions(btnRefresh)
                .setToolbarPosition(ActionToolbarPosition.BOTTOM)
                .createPanel()

        initSelectedAppText()

        apply {
            add(pnlAppTable, "growx")
            add(txtSelectedApp)
        }
    }

    fun fillAppsTable(apps: List<ResourceEx<T>>, defaultComparator: (T) -> Boolean = { false }) {
        btnRefresh.isEnabled = true
        table.emptyText.text = message("run_config.publish.form.existing_app.table.empty_message")

        val sortedApps = apps.sortedWith(compareBy (
                { it.resource.operatingSystem() },
                { it.subscriptionId },
                { it.resource.resourceGroupName() },
                { it.resource.name() }))

        cachedAppList = sortedApps

        if (sortedApps.isEmpty()) return

        val tableModel = table.model as DefaultTableModel
        tableModel.dataVector.clear()

        val subscriptionManager = AuthMethodManager.getInstance().azureManager.subscriptionManager

        for (appIndex in sortedApps.indices) {
            val app = sortedApps[appIndex].resource
            val subscription = subscriptionManager.subscriptionIdToSubscriptionMap[app.manager().subscriptionId()] ?: continue
            val icon = when (app.operatingSystem()) {
                OperatingSystem.WINDOWS -> osWindowsIcon
                OperatingSystem.LINUX -> osLinuxIcon
                else -> null
            }

            tableModel.addRow(arrayOf(
                    app.name(),
                    app.resourceGroupName(),
                    app.region().label(),
                    icon ?: "",
                    mapAppFrameworkVersion(app),
                    subscription.displayName())
            )

            if (defaultComparator(app) || (lastSelectedResource != null && lastSelectedResource?.resource?.id() == app.id())) {
                table.setRowSelectionInterval(appIndex, appIndex)
            }
        }
    }

    private fun initExistingAppsTable(): JBTable {
        val tableModel = object : DefaultTableModel() {
            override fun isCellEditable(row: Int, column: Int) = false
        }

        tableModel.addColumn(appTableColumnName)
        tableModel.addColumn(appTableColumnResourceGroup)
        tableModel.addColumn(appTableColumnLocation)
        tableModel.addColumn(appTableColumnOs)
        tableModel.addColumn(appTableColumnRuntime)
        tableModel.addColumn(appTableColumnSubscription)

        val table = object : JBTable(tableModel) {
            override fun getPreferredScrollableViewportSize(): Dimension? {
                val minHeight = JBUI.scale(200)
                val subtractHeight = JBUI.scale(430) // Height of all components, but web apps table (experimental value)
                return Dimension(-1, Math.max(topLevelAncestor?.height?.minus(subtractHeight) ?: minHeight, minHeight))
            }
        }

        table.emptyText.text = tableLoadingMessage
        table.rowSelectionAllowed = true
        table.setShowGrid(false)
        table.isStriped = true
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

        table.rowSorter = TableRowSorter(table.model)

        val osColumn = table.getColumn(appTableColumnOs)
        osColumn.cellRenderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
                val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                (component as JLabel).icon = value as? Icon
                component.text = ""
                component.horizontalAlignment = SwingConstants.CENTER
                return component
            }
        }

        table.selectionModel.addListSelectionListener { event ->
            if (event.valueIsAdjusting) return@addListSelectionListener

            val selectedRow = table.selectedRow

            if (cachedAppList.isEmpty() || selectedRow < 0 || selectedRow >= cachedAppList.size) {
                lastSelectedResource = null
                txtSelectedApp.text = ""
                return@addListSelectionListener
            }

            val columns = table.columnModel.columns.toList().map { column -> column.identifier as String }
            val resourceGroupIndex = columns.indexOf(appTableColumnResourceGroup)
            val appNameIndex = columns.indexOf(appTableColumnName)

            val appResourceGroup = table.getValueAt(selectedRow, resourceGroupIndex)
            val appName = table.getValueAt(selectedRow, appNameIndex)

            val resource = cachedAppList.find { appResource ->
                appResource.resource.name() == appName &&
                        appResource.resource.resourceGroupName() == appResourceGroup
            }

            lastSelectedResource = resource

            // This is not visible on the UI, but is used to preform a re-validation over selected web app from the table
            val app = resource?.resource
            txtSelectedApp.text = app?.name() ?: ""

            if (app != null)
                tableSelectAction(app)
        }

        return table
    }

    private fun initRefreshButton() =
            object : AnActionButton(message("run_config.publish.form.existing_app.table.refresh"), AllIcons.Actions.Refresh) {
                override fun actionPerformed(anActionEvent: AnActionEvent) {
                    resetWidget()
                    tableRefreshAction()
                }
            }

    private fun resetWidget() {
        btnRefresh.isEnabled = false
        val model = table.model as DefaultTableModel
        model.dataVector.clear()
        model.fireTableDataChanged()
        table.emptyText.text = tableLoadingMessage
        txtSelectedApp.text = ""
    }

    private fun initSelectedAppText() {
        setComponentsVisible(false, txtSelectedApp)
    }

    /**
     * Get a user friendly text for net framework for an existing Azure web app base instance
     *
     * Note: Windows instances - there are two valid .NET Framework values for Windows instances (v4.7 and v3.5).
     *       Azure SDK provides a CLR values set in "netFrameworkVersion" property. It has one of two CLR values:
     *       - "v4.0" which refers to .Net Framework 4.7 version
     *       - "v2.0" which refers to .Net Framework 3.5 version
     *       We do mapping using this rules above to display it to a user.
     *
     *       Linux instances - we get the runtime value stored in "linuxFxVersion" property. Possible values can be
     *       found by running "az webapp list-runtimes --linux" in Azure CLI.
     *       Result example for dotnet containers: "DOTNETCORE|2.1", "DOTNETCORE|3.1"
     *       Return the value as is.
     *
     * @param app web app base instance
     * @return [String] with a .Net Framework version for a web app base instance
     */
    private fun mapAppFrameworkVersion(app: T): String =
            when (app.operatingSystem()) {
                OperatingSystem.WINDOWS -> {
                    val version = app.netFrameworkVersion().toString()
                    if (version.startsWith("v4")) "v4.7"
                    else "v3.5"
                }
                OperatingSystem.LINUX -> {
                    app.linuxFxVersion()
                }
                else -> ""
            }
}
