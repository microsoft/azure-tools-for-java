package com.microsoft.intellij.runner.db.dbconfig

import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.project.Project
import com.microsoft.azure.management.appservice.WebApp
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.utils.AzureUIRefreshCore
import com.microsoft.azuretools.utils.AzureUIRefreshEvent
import com.microsoft.intellij.runner.AzureRunProfileState
import com.microsoft.intellij.runner.RunProcessHandler
import com.microsoft.intellij.runner.db.AzureDatabaseMvpModel
import com.microsoft.intellij.runner.db.AzureDatabaseSettingModel
import java.net.URI

class RiderDatabaseRunState(project: Project,
                            private val myModel: AzureDatabaseSettingModel) : AzureRunProfileState<SqlDatabase>(project) {

    companion object {
        private const val TARGET_NAME = "SqlDatabase"

        private const val SUBSCRIPTION_ID_NOT_DEFIED = "Subscription ID is not defined"

        private const val SQL_DATABASE_CREATE = "Creating SQL Database '%s'..."
        private const val SQL_DATABASE_CREATE_SUCCESSFUL = "SQL Database is created successfully."
        private const val SQL_DATABASE_NAME_NOT_DEFINED = "SQL Database Name is not defined"
        private const val SQL_DATABASE_URL = "Please see SQL Database details by URL: %s"

        private const val SQL_SERVER_CREATE = "Creating SQL Server '%s'..."
        private const val SQL_SERVER_GET_EXISTING = "Get existing SQL Server with Id: '%s'"
        private const val SQL_SERVER_CREATE_SUCCESSFUL = "SQL Server is created, id: '%s'"
        private const val SQL_SERVER_NAME_NOT_DEFINED = "SQL Server Name is not defined"
        private const val SQL_SERVER_REGION_NOT_DEFINED = "SQL Server Region is not defined"
        private const val SQL_SERVER_RESOURCE_GROUP_NAME_NOT_DEFINED = "SQL Server Resource Group Name is not defined"
        private const val SQL_SERVER_ADMIN_LOGIN_NOT_DEFINED = "SQL Server Admin Login is not defined"
        private const val SQL_SERVER_ADMIN_PASSWORD_NOT_DEFINED = "SQL Server Admin Password is not defined"
        private const val SQL_SERVER_ID_NOT_DEFINED = "SQL Server ID is not defined"
    }

    override fun getDeployTarget(): String {
        return TARGET_NAME
    }

    override fun updateTelemetryMap(telemetryMap: MutableMap<String, String>) { }

    /**
     * Execute a Azure Publish run configuration step. The step creates [WebApp] azure instance with
     * new or existing web app and deploy the selected project to this web app.
     *
     * @param processHandler - a process handler to show a process message
     * @param telemetryMap - a key-value map for collecting telemetry
     *
     * @throws [Exception] exception during step execution
     * @return [WebApp] or null instance with a new azure web app that contains deployed application
     */
    @Throws(Exception::class)
    public override fun executeSteps(processHandler: RunProcessHandler,
                                     telemetryMap: MutableMap<String, String>): SqlDatabase? {

        val sqlServer = getOrCreateSqlServerFromConfiguration(myModel, processHandler)
        val database = createDatabase(sqlServer, myModel, processHandler)
        val subscriptionId = myModel.subscriptionId

        val databaseUri = getSqlDatabaseUri(subscriptionId, database)
        if (databaseUri != null)
            processHandler.setText(String.format(SQL_DATABASE_URL, databaseUri))

        return database
    }

    //region Handlers

    override fun onSuccess(result: SqlDatabase?, processHandler: RunProcessHandler) {
        processHandler.notifyComplete()
        if (AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, null))
        }

        if (result != null) {
            updateConfigurationDataModel()
        }
    }

    override fun onFail(errorMessage: String, processHandler: RunProcessHandler) {
        processHandler.println(errorMessage, ProcessOutputTypes.STDERR)
        processHandler.notifyComplete()
    }

    //endregion Handlers

    //region SQL Server

    /**
     * Get or create an Azure SQL server based on database model [AzureDatabaseSettingModel] parameters
     *
     * @param model database model
     * @param processHandler a process handler to show a process message
     *
     * @return [SqlServer] instance of existing or a new Azure SQL Server
     */
    private fun getOrCreateSqlServerFromConfiguration(model: AzureDatabaseSettingModel,
                                                      processHandler: RunProcessHandler): SqlServer {

        if (model.subscriptionId.isEmpty()) throw Exception(SUBSCRIPTION_ID_NOT_DEFIED)

        if (model.isCreatingSqlServer) {
            processHandler.setText(String.format(SQL_SERVER_CREATE, model.sqlServerName))

            if (model.sqlServerName.isEmpty()) throw Exception(SQL_SERVER_NAME_NOT_DEFINED)
            if (model.location.isEmpty()) throw Exception(SQL_SERVER_REGION_NOT_DEFINED)
            if (model.resourceGroupName.isEmpty()) throw Exception(SQL_SERVER_RESOURCE_GROUP_NAME_NOT_DEFINED)
            if (model.sqlServerAdminLogin.isEmpty()) throw Exception(SQL_SERVER_ADMIN_LOGIN_NOT_DEFINED)
            if (model.sqlServerAdminPassword.isEmpty()) throw Exception(SQL_SERVER_ADMIN_PASSWORD_NOT_DEFINED)

            val sqlServer = AzureDatabaseMvpModel.createSqlServer(
                    model.subscriptionId,
                    model.sqlServerName,
                    model.location,
                    model.isCreatingResourceGroup,
                    model.resourceGroupName,
                    model.sqlServerAdminLogin,
                    model.sqlServerAdminPassword)

            processHandler.setText(String.format(SQL_SERVER_CREATE_SUCCESSFUL, sqlServer.id()))

            return sqlServer
        }

        processHandler.setText(String.format(SQL_SERVER_GET_EXISTING, model.sqlServerId))

        if (model.sqlServerId.isEmpty()) throw Exception(SQL_SERVER_ID_NOT_DEFINED)
        return AzureDatabaseMvpModel.getSqlServerById(model.subscriptionId, model.sqlServerId)
    }

    //endregion SQL Server

    //region SQL Database

    /**
     * Create a web app from a [AzureDatabaseSettingModel] instance
     *
     * @param sqlServer instance of Azure SQL Server that will host a database
     * @param model database model
     * @param processHandler a process handler to show a process message
     *
     * @return [SqlDatabase] instance of a new or existing Azure SQL Database
     */
    private fun createDatabase(sqlServer: SqlServer,
                               model: AzureDatabaseSettingModel,
                               processHandler: RunProcessHandler): SqlDatabase {

        processHandler.setText(String.format(SQL_DATABASE_CREATE, model.databaseName))

        if (model.databaseName.isEmpty()) throw Exception(SQL_DATABASE_NAME_NOT_DEFINED)
        val database = AzureDatabaseMvpModel.createSqlDatabase(sqlServer, model.databaseName, model.collation)

        processHandler.setText(String.format(SQL_DATABASE_CREATE_SUCCESSFUL, database.id()))
        return database
    }

    /**
     * Get database URI from a published SQL Database
     *
     * @param database published database instance
     * @return [java.net.URI] to a SQL Database on Azure portal
     */
    private fun getSqlDatabaseUri(subscriptionId: String, database: SqlDatabase): URI? {
        val azureManager = AuthMethodManager.getInstance().azureManager
        val portalUrl = azureManager.portalUrl

        // Note: [SubscriptionManager.getSubscriptionTenant()] method does not update Subscription to TenantId map while
        //       [SubscriptionManager.getSubscriptionDetails()] force to update and get the correct value
        val tenantId = azureManager.subscriptionManager.subscriptionDetails
                .find { it.subscriptionId == subscriptionId }?.tenantId ?: return null

        val path = "/#@$tenantId/resource/${database.id()}/overview".replace("/+".toRegex(), "/")
        return URI.create("$portalUrl/$path").normalize()
    }

    /**
     * Reset [AzureDatabaseSettingModel] after a SQL database was deployed
     */
    private fun updateConfigurationDataModel() {
        myModel.reset()
    }

    //endregion SQL Database
}
