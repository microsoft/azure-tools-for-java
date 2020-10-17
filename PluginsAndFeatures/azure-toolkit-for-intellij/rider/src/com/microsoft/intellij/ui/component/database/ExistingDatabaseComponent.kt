/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.ui.component.database

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBPasswordField
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.extension.fillComboBox
import com.microsoft.intellij.ui.extension.getSelectedValue
import com.microsoft.intellij.ui.extension.setDefaultRenderer
import icons.CommonIcons
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.lang.IllegalStateException
import javax.swing.JLabel
import javax.swing.JPanel

class ExistingDatabaseComponent :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]")),
        AzureComponent {

    private val lblExistingDatabase = JLabel(message("run_config.publish.form.existing_db.name"))
    val cbDatabase = ComboBox<SqlDatabase>()

    private val lblExistingAdminLogin = JLabel(message("run_config.publish.form.existing_db.admin_login.label"))
    val lblExistingAdminLoginValue = JLabel(message("common.na_capitalized"))

    private val lblExistingAdminPassword = JLabel(message("run_config.publish.form.existing_db.admin_password.label"))
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
                message("run_config.publish.form.existing_db.empty_message"),
                CommonIcons.Database) { database -> "${database.name()} (${database.resourceGroupName()})" }

        cbDatabase.addActionListener {
            val database = cbDatabase.getSelectedValue() ?: return@addActionListener
            if (lastSelectedDatabase == database) return@addActionListener

            val id = database.id()
            val parts = id.split("/")
            val subscriptionIndex = parts.indexOf("subscriptions")
            if (subscriptionIndex == -1)
                throw IllegalStateException("Unable to collect information about subscription from database ID: '$id'")

            val subscriptionId = parts[subscriptionIndex + 1]

            ApplicationManager.getApplication().invokeLater {
                val sqlServer =
                        AzureSqlServerMvpModel.listSqlServersBySubscriptionId(subscriptionId)
                                .first { sqlServer -> sqlServer.name() == database.sqlServerName() }
                lblExistingAdminLoginValue.text = sqlServer.administratorLogin()
            }

            lastSelectedDatabase = database
        }
    }
}
