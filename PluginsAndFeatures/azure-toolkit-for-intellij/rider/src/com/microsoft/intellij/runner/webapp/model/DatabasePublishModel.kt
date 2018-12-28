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

package com.microsoft.intellij.runner.webapp.model

import com.intellij.util.xmlb.annotations.Transient
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.sql.SqlDatabase

class DatabasePublishModel {

    companion object {
        val defaultLocation: String = Region.US_EAST.name()
        const val defaultCollation = "SQL_Latin1_General_CP1_CI_AS"
    }

    var subscription: Subscription? = null

    var isDatabaseConnectionEnabled = false

    var connectionStringName = ""
    var database: SqlDatabase? = null

    var isCreatingSqlDatabase = false

    var databaseName = ""

    var isCreatingDbResourceGroup = true
    var dbResourceGroupName = ""

    var isCreatingSqlServer = true
    var sqlServerId = ""
    var sqlServerName = ""
    var sqlServerAdminLogin = ""

    @get:Transient
    var sqlServerAdminPassword = charArrayOf()

    @get:Transient
    var sqlServerAdminPasswordConfirm = charArrayOf()

    var sqlServerLocation = defaultLocation

    var collation = defaultCollation

    /**
     * Reset the model with values after creating a new instance
     */
    fun resetOnPublish(sqlDatabase: SqlDatabase) {
        isDatabaseConnectionEnabled = true

        isCreatingSqlDatabase = false
        database = sqlDatabase

        isCreatingDbResourceGroup = false
        isCreatingSqlServer = false
    }
}