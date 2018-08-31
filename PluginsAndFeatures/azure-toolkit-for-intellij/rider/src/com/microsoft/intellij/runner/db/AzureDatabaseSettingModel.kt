package com.microsoft.intellij.runner.db

import com.microsoft.azure.management.sql.DatabaseEditions

class AzureDatabaseSettingModel {

    var subscriptionId: String? = null

    var databaseName: String? = null

    var isCreatingResourceGroup = false
    var resourceGroupName: String? = null

    var isCreatingSqlServer = false
    var sqlServerId: String? = null
    var sqlServerName: String? = null
    var sqlServerAdminLogin: String? = null
    var sqlServerAdminPassword: CharArray? = null
    var sqlServerAdminPasswordConfirm: CharArray? = null

    var region: String? = null
    var databaseEdition: DatabaseEditions? = null

    var collation: String = "SQL_Latin1_General_CP1_CI_AS"

    fun reset() {
        subscriptionId = null
        databaseName = null
        isCreatingResourceGroup = false
        resourceGroupName = null
        isCreatingSqlServer = false
        sqlServerId = null
        sqlServerName = null
        sqlServerAdminLogin = null
        sqlServerAdminPassword = null
        sqlServerAdminPasswordConfirm = null
        region = null
        databaseEdition = null
        collation = "SQL_Latin1_General_CP1_CI_AS"
    }
}