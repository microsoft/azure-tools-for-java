package com.microsoft.intellij.runner.db

import com.microsoft.azure.management.sql.DatabaseEditions

class AzureDatabaseSettingModel {

    var subscriptionId: String = ""

    var databaseName: String = ""

    var isCreatingResourceGroup = true
    var resourceGroupName: String = ""

    var isCreatingSqlServer = true
    var sqlServerId: String = ""
    var sqlServerName: String = ""
    var sqlServerAdminLogin: String = ""
    var sqlServerAdminPassword: CharArray = charArrayOf()
    var sqlServerAdminPasswordConfirm: CharArray = charArrayOf()

    var region: String = ""
    var databaseEdition: DatabaseEditions = defaultDatabaseEditions

    var collation: String = defaultCollation

    fun reset() {
        subscriptionId = ""
        databaseName = ""
        isCreatingResourceGroup = false
        resourceGroupName = ""
        isCreatingSqlServer = false
        sqlServerId = ""
        sqlServerName = ""
        sqlServerAdminLogin = ""
        sqlServerAdminPassword = charArrayOf()
        sqlServerAdminPasswordConfirm = charArrayOf()
        region = ""
        databaseEdition = defaultDatabaseEditions
        collation = defaultCollation
    }

    companion object {
        const val defaultCollation = "SQL_Latin1_General_CP1_CI_AS"
        val defaultDatabaseEditions: DatabaseEditions = DatabaseEditions.STANDARD
    }
}