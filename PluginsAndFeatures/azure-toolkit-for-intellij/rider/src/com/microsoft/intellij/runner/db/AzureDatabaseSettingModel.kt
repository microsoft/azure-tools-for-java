package com.microsoft.intellij.runner.db

import com.intellij.util.xmlb.annotations.Transient
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.sql.DatabaseEditions

class AzureDatabaseSettingModel {

    var subscriptionId = ""

    var databaseName = ""

    var isCreatingResourceGroup = true
    var resourceGroupName = ""

    var isCreatingSqlServer = true
    var sqlServerId = ""
    var sqlServerName = ""
    var sqlServerAdminLogin = ""

    @get:Transient
    var sqlServerAdminPassword = charArrayOf()

    @get:Transient
    var sqlServerAdminPasswordConfirm = charArrayOf()

    var location = defaultLocation
    var databaseEdition = defaultDatabaseEditions

    var collation = defaultCollation

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
        location = defaultLocation
        databaseEdition = defaultDatabaseEditions
        collation = defaultCollation
    }

    companion object {
        val defaultLocation: String = Region.US_EAST.name()
        const val defaultCollation = "SQL_Latin1_General_CP1_CI_AS"
        val defaultDatabaseEditions: DatabaseEditions = DatabaseEditions.STANDARD
    }
}