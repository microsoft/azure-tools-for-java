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

package com.microsoft.intellij.runner.database.model

import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.util.xmlb.annotations.Transient
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.intellij.helpers.defaults.AzureDefaults
import org.jdom.Element

class DatabasePublishModel {

    companion object {
        private const val AZURE_SQL_DATABASE_SUBSCRIPTION_ID          = "AZURE_SQL_DATABASE_SUBSCRIPTION_ID"
        private const val AZURE_SQL_DATABASE_IS_ENABLED               = "AZURE_SQL_DATABASE_IS_ENABLED"
        private const val AZURE_SQL_DATABASE_CONNECTION_STRING_NAME   = "AZURE_SQL_DATABASE_CONNECTION_STRING_NAME"
        private const val AZURE_SQL_DATABASE_IS_CREATE                = "AZURE_SQL_DATABASE_IS_CREATE"
        private const val AZURE_SQL_DATABASE_ID                       = "AZURE_SQL_DATABASE_ID"
        private const val AZURE_SQL_DATABASE_NAME                     = "AZURE_SQL_DATABASE_NAME"
        private const val AZURE_SQL_DATABASE_IS_CREATE_RESOURCE_GROUP = "AZURE_SQL_DATABASE_IS_CREATE_RESOURCE_GROUP"
        private const val AZURE_SQL_DATABASE_RESOURCE_GROUP_NAME      = "AZURE_SQL_DATABASE_RESOURCE_GROUP_NAME"
        private const val AZURE_SQL_DATABASE_IS_CREATE_SQL_SERVER     = "AZURE_SQL_DATABASE_IS_CREATE_SQL_SERVER"
        private const val AZURE_SQL_DATABASE_SQL_SERVER_ID            = "AZURE_SQL_DATABASE_SQL_SERVER_ID"
        private const val AZURE_SQL_DATABASE_SQL_SERVER_NAME          = "AZURE_SQL_DATABASE_SQL_SERVER_NAME"
        private const val AZURE_SQL_DATABASE_SQL_SERVER_ADMIN_LOGIN   = "AZURE_SQL_DATABASE_SQL_SERVER_ADMIN_LOGIN"
        private const val AZURE_SQL_DATABASE_LOCATION                 = "AZURE_SQL_DATABASE_LOCATION"
        private const val AZURE_SQL_DATABASE_COLLATION                = "AZURE_SQL_DATABASE_COLLATION"
    }

    var subscription: Subscription? = null

    var isDatabaseConnectionEnabled = false

    var connectionStringName = ""

    var isCreatingSqlDatabase = false
    var databaseId = ""
    var databaseName = ""

    var isCreatingResourceGroup = false
    var resourceGroupName = ""

    var isCreatingSqlServer = false
    var sqlServerId = ""
    var sqlServerName = ""
    var sqlServerAdminLogin = ""

    @get:Transient
    var sqlServerAdminPassword = charArrayOf()

    @get:Transient
    var sqlServerAdminPasswordConfirm = charArrayOf()

    var location = AzureDefaults.location

    var collation = AzureDefaults.SQL_DATABASE_COLLATION

    /**
     * Reset the model with values after creating a new instance
     */
    fun resetOnPublish(sqlDatabase: SqlDatabase) {
        isDatabaseConnectionEnabled = true

        isCreatingSqlDatabase = false
        databaseId = sqlDatabase.id()

        isCreatingResourceGroup = false
        resourceGroupName = ""

        isCreatingSqlServer = false
        sqlServerName = ""
    }

    fun readExternal(element: Element) {
        val subscriptionId = JDOMExternalizerUtil.readField(element, AZURE_SQL_DATABASE_SUBSCRIPTION_ID) ?: ""
        subscription = AzureMvpModel.getInstance().selectedSubscriptions.find { it.subscriptionId() == subscriptionId }

        isDatabaseConnectionEnabled = JDOMExternalizerUtil.readField(element, AZURE_SQL_DATABASE_IS_ENABLED) == "1"
        connectionStringName = JDOMExternalizerUtil.readField(element, AZURE_SQL_DATABASE_CONNECTION_STRING_NAME) ?: ""

        isCreatingSqlDatabase = JDOMExternalizerUtil.readField(element, AZURE_SQL_DATABASE_IS_CREATE) == "1"
        databaseId = JDOMExternalizerUtil.readField(element, AZURE_SQL_DATABASE_ID) ?: ""
        databaseName = JDOMExternalizerUtil.readField(element, AZURE_SQL_DATABASE_NAME) ?: ""

        isCreatingResourceGroup = JDOMExternalizerUtil.readField(element, AZURE_SQL_DATABASE_IS_CREATE_RESOURCE_GROUP) == "1"
        resourceGroupName = JDOMExternalizerUtil.readField(element, AZURE_SQL_DATABASE_RESOURCE_GROUP_NAME) ?: ""

        isCreatingSqlServer = JDOMExternalizerUtil.readField(element, AZURE_SQL_DATABASE_IS_CREATE_SQL_SERVER) == "1"
        sqlServerId = JDOMExternalizerUtil.readField(element, AZURE_SQL_DATABASE_SQL_SERVER_ID) ?: ""
        sqlServerName = JDOMExternalizerUtil.readField(element, AZURE_SQL_DATABASE_SQL_SERVER_NAME) ?: ""
        sqlServerAdminLogin = JDOMExternalizerUtil.readField(element, AZURE_SQL_DATABASE_SQL_SERVER_ADMIN_LOGIN) ?: ""

        val locationName = JDOMExternalizerUtil.readField(element, AZURE_SQL_DATABASE_LOCATION) ?: AzureDefaults.location.name()
        location = Region.fromName(locationName)

        collation = JDOMExternalizerUtil.readField(element, AZURE_SQL_DATABASE_COLLATION) ?: ""
    }

    fun writeExternal(element: Element) {
        JDOMExternalizerUtil.writeField(element, AZURE_SQL_DATABASE_SUBSCRIPTION_ID, subscription?.subscriptionId() ?: "")

        JDOMExternalizerUtil.writeField(element, AZURE_SQL_DATABASE_IS_ENABLED, if (isDatabaseConnectionEnabled) "1" else "0")
        JDOMExternalizerUtil.writeField(element, AZURE_SQL_DATABASE_CONNECTION_STRING_NAME, connectionStringName)

        JDOMExternalizerUtil.writeField(element, AZURE_SQL_DATABASE_IS_CREATE, if (isCreatingSqlDatabase) "1" else "0")
        JDOMExternalizerUtil.writeField(element, AZURE_SQL_DATABASE_ID, databaseId)
        JDOMExternalizerUtil.writeField(element, AZURE_SQL_DATABASE_NAME, databaseName)

        JDOMExternalizerUtil.writeField(element, AZURE_SQL_DATABASE_IS_CREATE_RESOURCE_GROUP, if (isCreatingResourceGroup) "1" else "0")
        JDOMExternalizerUtil.writeField(element, AZURE_SQL_DATABASE_RESOURCE_GROUP_NAME, resourceGroupName)

        JDOMExternalizerUtil.writeField(element, AZURE_SQL_DATABASE_IS_CREATE_SQL_SERVER, if (isCreatingSqlServer) "1" else "0")
        JDOMExternalizerUtil.writeField(element, AZURE_SQL_DATABASE_SQL_SERVER_ID, sqlServerId)
        JDOMExternalizerUtil.writeField(element, AZURE_SQL_DATABASE_SQL_SERVER_NAME, sqlServerName)
        JDOMExternalizerUtil.writeField(element, AZURE_SQL_DATABASE_SQL_SERVER_ADMIN_LOGIN, sqlServerAdminLogin)
        JDOMExternalizerUtil.writeField(element, AZURE_SQL_DATABASE_LOCATION, location.name())
        JDOMExternalizerUtil.writeField(element, AZURE_SQL_DATABASE_COLLATION, collation)
    }
}
