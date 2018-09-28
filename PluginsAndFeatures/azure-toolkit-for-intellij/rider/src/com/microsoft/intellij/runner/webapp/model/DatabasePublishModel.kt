package com.microsoft.intellij.runner.webapp.model

import com.intellij.util.xmlb.annotations.Transient
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.sql.DatabaseEditions
import com.microsoft.azure.management.sql.SqlDatabase

class DatabasePublishModel {

    companion object {
        val defaultLocation: String = Region.US_EAST.name()
        const val defaultCollation = "SQL_Latin1_General_CP1_CI_AS"
        val defaultDatabaseEditions: DatabaseEditions = DatabaseEditions.STANDARD
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
    var databaseEdition = defaultDatabaseEditions

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