/**
 * Copyright (c) 2019 JetBrains s.r.o.
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

package com.microsoft.intellij.component.appservice

import com.intellij.icons.AllIcons
import com.intellij.ui.border.IdeaTitledBorder
import com.intellij.util.ui.JBUI
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azure.management.appservice.WebAppBase
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.intellij.component.AzureComponent
import net.miginfocom.swing.MigLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.RowFilter.regexFilter
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

class AppExistingComponent<T : WebAppBase> :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1")),
        AzureComponent {

    companion object {
        private val warningIcon = AllIcons.General.BalloonWarning
    }

    val pnlExistingAppTable = ExistingAppsTableComponent<T>()
    private val lblRuntimeMismatchWarning = JLabel()

    init {
        initRuntimeMismatchWarningLabel()
        initRefreshButton()

        border = IdeaTitledBorder("Choose App", 0, JBUI.emptyInsets())
        add(pnlExistingAppTable, "growx")
        add(lblRuntimeMismatchWarning, "growx")

        initComponentValidation()
    }

    fun setRuntimeMismatchWarning(show: Boolean, message: String = "") {
        lblRuntimeMismatchWarning.text = message
        lblRuntimeMismatchWarning.isVisible = show
    }

    fun fillAppsTable(apps: List<ResourceEx<T>>, defaultAppId: String? = null) {
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

        val osColumnIndex = (pnlExistingAppTable.table.model as DefaultTableModel).findColumn(ExistingAppsTableComponent.APP_TABLE_COLUMN_OS)
        require(osColumnIndex >= 0) { "Column index is out of range" }
        sorter.rowFilter = regexFilter(OperatingSystem.WINDOWS.name.toLowerCase().capitalize(), osColumnIndex)
    }

    private fun initRefreshButton() {
        val action = pnlExistingAppTable.tableRefreshAction

        pnlExistingAppTable.tableRefreshAction = {
            setRuntimeMismatchWarning(false)
            action()
        }
    }

    private fun initRuntimeMismatchWarningLabel() {
        lblRuntimeMismatchWarning.icon = warningIcon
        setRuntimeMismatchWarning(false)
    }
}