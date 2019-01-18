/**
 * Copyright (c) 2018 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner.webapp.webappconfig.ui.component.webapp

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.AnActionButton
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azure.management.appservice.WebApp
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.intellij.component.AzureComponent
import com.microsoft.intellij.component.extension.setComponentsVisible
import net.miginfocom.swing.MigLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

class WebAppTableComponent :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1, hidemode 3")),
        AzureComponent {

    companion object {

        const val WEB_APP_TABLE_COLUMN_SUBSCRIPTION = "Subscription"
        const val WEB_APP_TABLE_COLUMN_NAME = "Name"
        const val WEB_APP_TABLE_COLUMN_RESOURCE_GROUP = "Resource group"
        const val WEB_APP_TABLE_COLUMN_LOCATION = "Location"
        const val WEB_APP_TABLE_COLUMN_OS = "OS"
        const val WEB_APP_TABLE_COLUMN_DOTNET_VERSION = ".Net Version"

        private const val BUTTON_REFRESH_NAME = "Refresh"

        private const val TABLE_LOADING_MESSAGE = "Loading ... "
        private const val TABLE_EMPTY_MESSAGE = "No available Web Apps."

        private val osWindowsIcon = IconLoader.getIcon("icons/OSWindows.svg")
        private val osLinuxIcon = IconLoader.getIcon("icons/OSLinux.svg")
    }

    val table = initWebAppTable()
    val btnRefresh = initRefreshButton()
    var tableRefreshAction = {}
    var tableSelectAction: (WebApp) -> Unit = {}
    private val pnlWebAppTable: JPanel
    private val txtSelectedWebApp = JTextField()

    var lastSelectedWebApp: WebApp? = null
    var cachedWebAppList = listOf<ResourceEx<WebApp>>()

    init {
        pnlWebAppTable = ToolbarDecorator
                .createDecorator(table)
                .addExtraActions(btnRefresh)
                .setToolbarPosition(ActionToolbarPosition.BOTTOM)
                .createPanel()

        initSelectedWebAppText()

        add(pnlWebAppTable, "growx")
        add(txtSelectedWebApp)
    }

    fun fillWebAppTable(webApps: List<ResourceEx<WebApp>>, defaultComparator: (WebApp) -> Boolean = { false }) {
        btnRefresh.isEnabled = true
        table.emptyText.text = TABLE_EMPTY_MESSAGE

        val sortedWebApps = webApps.sortedWith(compareBy (
                { it.resource.operatingSystem() },
                { it.subscriptionId },
                { it.resource.resourceGroupName() },
                { it.resource.name() }))

        cachedWebAppList = sortedWebApps

        if (sortedWebApps.isEmpty()) return

        val tableModel = table.model as DefaultTableModel
        tableModel.dataVector.clear()

        val subscriptionManager = AuthMethodManager.getInstance().azureManager.subscriptionManager

        for (i in sortedWebApps.indices) {
            val webApp = sortedWebApps[i].resource
            val subscription = subscriptionManager.subscriptionIdToSubscriptionMap[webApp.manager().subscriptionId()] ?: continue
            val icon = when (webApp.operatingSystem()) {
                OperatingSystem.WINDOWS -> osWindowsIcon
                OperatingSystem.LINUX -> osLinuxIcon
                else -> null
            }

            tableModel.addRow(arrayOf(
                    webApp.name(),
                    webApp.resourceGroupName(),
                    webApp.region().label(),
                    icon ?: "",
                    mapWebAppFrameworkVersion(webApp),
                    subscription.displayName())
            )

            if (defaultComparator(webApp) ||
                    (lastSelectedWebApp != null && lastSelectedWebApp?.id() == webApp.id())) {
                table.setRowSelectionInterval(i, i)
            }
        }
    }

    private fun initWebAppTable(): JBTable {
        val tableModel = object : DefaultTableModel() {
            override fun isCellEditable(row: Int, column: Int) = false
        }

        tableModel.addColumn(WEB_APP_TABLE_COLUMN_NAME)
        tableModel.addColumn(WEB_APP_TABLE_COLUMN_RESOURCE_GROUP)
        tableModel.addColumn(WEB_APP_TABLE_COLUMN_LOCATION)
        tableModel.addColumn(WEB_APP_TABLE_COLUMN_OS)
        tableModel.addColumn(WEB_APP_TABLE_COLUMN_DOTNET_VERSION)
        tableModel.addColumn(WEB_APP_TABLE_COLUMN_SUBSCRIPTION)

        val table = object : JBTable(tableModel) {
            override fun getPreferredScrollableViewportSize(): Dimension? {
                val minHeight = JBUI.scale(200)
                val subtractHeight = JBUI.scale(420) // Height of all components, but web apps table (experimental value)
                return Dimension(-1, Math.max(topLevelAncestor?.height?.minus(subtractHeight) ?: minHeight, minHeight))
            }
        }

        table.emptyText.text = TABLE_LOADING_MESSAGE
        table.rowSelectionAllowed = true
        table.setShowGrid(false)
        table.isStriped = true
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

        table.rowSorter = TableRowSorter<TableModel>(table.model)

        val osColumn = table.getColumn(WEB_APP_TABLE_COLUMN_OS)
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

            if (cachedWebAppList.isEmpty() || selectedRow < 0 || selectedRow >= cachedWebAppList.size) {
                lastSelectedWebApp = null
                txtSelectedWebApp.text = ""
                return@addListSelectionListener
            }

            val columns = table.columnModel.columns.toList().map { column -> column.identifier as String }
            val resourceGroupIndex = columns.indexOf(WebAppTableComponent.WEB_APP_TABLE_COLUMN_RESOURCE_GROUP)
            val webAppNameIndex = columns.indexOf(WebAppTableComponent.WEB_APP_TABLE_COLUMN_NAME)

            val webAppResourceGroup = table.getValueAt(selectedRow, resourceGroupIndex)
            val webAppName = table.getValueAt(selectedRow, webAppNameIndex)

            val webApp = cachedWebAppList.find { webAppRes ->
                webAppRes.resource.name() == webAppName &&
                        webAppRes.resource.resourceGroupName() == webAppResourceGroup
            }?.resource

            lastSelectedWebApp = webApp

            // This is not visible on the UI, but is used to preform a re-validation over selected web app from the table
            txtSelectedWebApp.text = webApp?.name() ?: ""

            if (webApp != null)
                tableSelectAction(webApp)
        }

        return table
    }

    private fun initRefreshButton() =
            object : AnActionButton(BUTTON_REFRESH_NAME, AllIcons.Actions.Refresh) {
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
        table.emptyText.text = TABLE_LOADING_MESSAGE
        txtSelectedWebApp.text = ""
    }

    private fun initSelectedWebAppText() {
        setComponentsVisible(false, txtSelectedWebApp)
    }

    /**
     * Get a user friendly text for net framework for an existing Azure web app instance
     *
     * Note: For .NET Framework there are two valid values for version (v4.7 and v3.5). Azure SDK
     *       does not set the right values. They set the "v4.0" for "v4.7" and "v2.0" for "v3.5" instances
     *       For .Net Core Framework we get the correct runtime value from the linux fx version that store
     *       a instance name representation
     *
     * Note: 2.0 and 4.0 are CLR versions most likely
     *
     * @param webApp web app instance
     * @return [String] with a .Net Framework version for a web app instance
     */
    private fun mapWebAppFrameworkVersion(webApp: WebApp): String {
        return if (webApp.operatingSystem() == OperatingSystem.LINUX) {
            "Core v${webApp.linuxFxVersion().split('|')[1]}"
        } else {
            val version = webApp.netFrameworkVersion().toString()
            if (version.startsWith("v4")) "v4.7"
            else "v3.5"
        }
    }
}
