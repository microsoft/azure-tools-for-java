/**
 * Copyright (c) 2018-2019 JetBrains s.r.o.
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

package com.microsoft.intellij.component.database

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.JBPasswordField
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel
import com.microsoft.intellij.component.AzureComponent
import com.microsoft.intellij.component.extension.setDefaultRenderer
import com.microsoft.intellij.component.extension.fillComboBox
import com.microsoft.intellij.component.extension.getSelectedValue
import net.miginfocom.swing.MigLayout
import javax.swing.JLabel
import javax.swing.JPanel

class ExistingDatabaseComponent :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]")),
        AzureComponent {

    companion object {
        private const val EMPTY_SQL_DATABASES_MESSAGE = "No existing Azure SQL Databases"
        private const val NOT_APPLICABLE = "N/A"
    }

    private val lblExistingDatabase = JLabel("Database")
    val cbDatabase = ComboBox<SqlDatabase>()
    private val lblExistingAdminLogin = JLabel("Admin Login")
    val lblExistingAdminLoginValue = JLabel(NOT_APPLICABLE)
    private val lblExistingAdminPassword = JLabel("Admin Password")
    val passExistingDbAdminPassword = JBPasswordField()

    var lastSelectedDatabase: SqlDatabase? = null

    init {
        initSqlDatabaseComboBox()

        add(lblExistingDatabase)
        add(cbDatabase, "growx")
        add(lblExistingAdminLogin)
        add(lblExistingAdminLoginValue, "growx")
        add(lblExistingAdminPassword)
        add(passExistingDbAdminPassword, "growx")
    }

    fun fillSqlDatabase(sqlDatabases: List<SqlDatabase>, defaultDatabaseId: String? = null) {
        cbDatabase.fillComboBox(
                elements = sqlDatabases.sortedBy { it.name() },
                defaultComparator = { sqlDatabase -> sqlDatabase.id() == defaultDatabaseId })

        if (sqlDatabases.isEmpty()) {
            lastSelectedDatabase = null
        }
    }

    private fun initSqlDatabaseComboBox() {
        cbDatabase.setDefaultRenderer(
                EMPTY_SQL_DATABASES_MESSAGE,
                IconLoader.getIcon("icons/Database.svg")) { database -> "${database.name()} (${database.resourceGroupName()})" }

        cbDatabase.addActionListener {
            val database = cbDatabase.getSelectedValue() ?: return@addActionListener
            if (lastSelectedDatabase == database) return@addActionListener

            val subscriptionId = database.id().split("/")[2]

            ApplicationManager.getApplication().invokeLater {
                val sqlServer =
                        AzureSqlServerMvpModel.listSqlServersBySubscriptionId(subscriptionId)
                                .first { subscription -> subscription.resource.name() == database.sqlServerName() }.resource
                lblExistingAdminLoginValue.text = sqlServer.administratorLogin()
            }

            lastSelectedDatabase = database
        }
    }
}
