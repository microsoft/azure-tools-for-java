/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.validator

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlDatabaseMvpModel
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel
import com.microsoft.intellij.runner.database.model.DatabasePublishModel
import org.jetbrains.mock.SqlDatabaseMock
import org.jetbrains.mock.SqlServerMock
import org.jetbrains.mock.SubscriptionMock
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class SqlDatabaseConfigValidatorTest {

    //region Db Connection Enabled

    @Test
    fun testValidateDatabaseConnection_DatabaseConnection_Disabled() {
        val model = DatabasePublishModel().apply { isDatabaseConnectionEnabled = false }
        SqlDatabaseConfigValidator.validateDatabaseConnection(model)
    }

    //endregion Db Connection Enabled

    //region New Db

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "SQL Database name is not defined\\.")
    fun testValidateDatabaseConnection_NameIsNotSet() {
        val model = DatabasePublishModel().apply {
            isDatabaseConnectionEnabled = true
            isCreatingSqlDatabase = true
            subscription = SubscriptionMock()
            databaseName = ""
        }

        SqlDatabaseConfigValidator.validateDatabaseConnection(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "SQL Database name 'test-sql-database' already exists\\.")
    fun testValidateDatabaseConnection_DatabaseExists() {
        val mockSubscription = SubscriptionMock()
        val mockSqlServer = SqlServerMock(name = "test-sql-server")
        AzureSqlServerMvpModel.setSubscriptionIdToSqlServersMap(mapOf(
                mockSubscription.subscriptionId() to listOf(mockSqlServer)
        ))

        val mockSqlDatabase = SqlDatabaseMock(name = "test-sql-database")
        AzureSqlDatabaseMvpModel.setSqlServerToSqlDatabasesMap(mapOf(
                mockSqlServer to listOf(mockSqlDatabase)
        ))

        val model = DatabasePublishModel().apply {
            isDatabaseConnectionEnabled = true
            isCreatingSqlDatabase = true
            subscription = mockSubscription
            databaseName = mockSqlDatabase.name()
            sqlServerName = mockSqlServer.name()
        }

        SqlDatabaseConfigValidator.validateDatabaseConnection(model)
    }

    @DataProvider(name = "validateFunctionAppNewResourceGroupNotSetData")
    fun validateFunctionAppNewResourceGroupNotSetData() = arrayOf(
            arrayOf("Existing", false),
            arrayOf("CreateNew", true)
    )

    @Test(dataProvider = "validateFunctionAppNewResourceGroupNotSetData",
            expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Resource Group name not provided\\.")
    fun testValidateDatabaseConnection_ResourceGroupIsNotSet(name: String, isCreateNewResourceGroup: Boolean) {
        val mockSubscription = SubscriptionMock()
        val mockSqlServer = SqlServerMock(name = "test-sql-server")
        AzureSqlServerMvpModel.setSubscriptionIdToSqlServersMap(mapOf(
                mockSubscription.subscriptionId() to listOf(mockSqlServer)
        ))

        val mockSqlDatabase = SqlDatabaseMock(name = "test-sql-database")
        AzureSqlDatabaseMvpModel.setSqlServerToSqlDatabasesMap(mapOf(
                mockSqlServer to listOf(mockSqlDatabase)
        ))

        val model = DatabasePublishModel().apply {
            isDatabaseConnectionEnabled = true
            isCreatingSqlDatabase = true
            subscription = mockSubscription
            databaseName = "test-sql-database-1"
            isCreatingResourceGroup = isCreateNewResourceGroup
            resourceGroupName = ""
        }

        SqlDatabaseConfigValidator.validateDatabaseConnection(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "SQL Server ID is not defined\\.")
    fun testValidateDatabaseConnection_SqlServerIdIsNotSet() {
        val mockSubscription = SubscriptionMock()
        val mockSqlServer = SqlServerMock(name = "test-sql-server")
        AzureSqlServerMvpModel.setSubscriptionIdToSqlServersMap(mapOf(
                mockSubscription.subscriptionId() to listOf(mockSqlServer)
        ))

        val mockSqlDatabase = SqlDatabaseMock(name = "test-sql-database")
        AzureSqlDatabaseMvpModel.setSqlServerToSqlDatabasesMap(mapOf(
                mockSqlServer to listOf(mockSqlDatabase)
        ))

        val model = DatabasePublishModel().apply {
            isDatabaseConnectionEnabled = true
            isCreatingSqlDatabase = true
            subscription = mockSubscription
            databaseName = "test-sql-database-1"
            isCreatingResourceGroup = false
            resourceGroupName = "test-resource-group"
            isCreatingSqlServer = false
            sqlServerId = ""
        }

        SqlDatabaseConfigValidator.validateDatabaseConnection(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "SQL Server Admin Password is not defined\\.")
    fun testValidateDatabaseConnection_SqlServerAdminPassIsNotSet() {
        val mockSubscription = SubscriptionMock()
        val mockSqlServer = SqlServerMock(name = "test-sql-server")
        AzureSqlServerMvpModel.setSubscriptionIdToSqlServersMap(mapOf(
                mockSubscription.subscriptionId() to listOf(mockSqlServer)
        ))

        val mockSqlDatabase = SqlDatabaseMock(name = "test-sql-database")
        AzureSqlDatabaseMvpModel.setSqlServerToSqlDatabasesMap(mapOf(
                mockSqlServer to listOf(mockSqlDatabase)
        ))

        val model = DatabasePublishModel().apply {
            isDatabaseConnectionEnabled = true
            isCreatingSqlDatabase = true
            subscription = mockSubscription
            databaseName = "test-sql-database-1"
            isCreatingResourceGroup = false
            resourceGroupName = "test-resource-group"
            isCreatingSqlServer = false
            sqlServerId = "test-sql-server"
            sqlServerAdminPassword = CharArray(0)
        }

        SqlDatabaseConfigValidator.validateDatabaseConnection(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "SQL Server Name is not defined\\.")
    fun testValidateDatabaseConnection_SqlServerNameIsNotSet() {
        val mockSubscription = SubscriptionMock()
        val mockSqlServer = SqlServerMock(name = "test-sql-server")
        AzureSqlServerMvpModel.setSubscriptionIdToSqlServersMap(mapOf(
                mockSubscription.subscriptionId() to listOf(mockSqlServer)
        ))

        val mockSqlDatabase = SqlDatabaseMock(name = "test-sql-database")
        AzureSqlDatabaseMvpModel.setSqlServerToSqlDatabasesMap(mapOf(
                mockSqlServer to listOf(mockSqlDatabase)
        ))

        val model = DatabasePublishModel().apply {
            isDatabaseConnectionEnabled = true
            isCreatingSqlDatabase = true
            subscription = mockSubscription
            databaseName = "test-sql-database-1"
            isCreatingResourceGroup = false
            resourceGroupName = "test-resource-group"
            isCreatingSqlServer = true
            sqlServerName = ""
        }

        SqlDatabaseConfigValidator.validateDatabaseConnection(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "SQL Server name 'test-sql-server' already exists\\.")
    fun testValidateDatabaseConnection_SqlServerExists() {
        val mockSubscription = SubscriptionMock()
        val mockSqlServer = SqlServerMock(name = "test-sql-server")
        AzureSqlServerMvpModel.setSubscriptionIdToSqlServersMap(mapOf(
                mockSubscription.subscriptionId() to listOf(mockSqlServer)
        ))

        val mockSqlDatabase = SqlDatabaseMock(name = "test-sql-database")
        AzureSqlDatabaseMvpModel.setSqlServerToSqlDatabasesMap(mapOf(
                mockSqlServer to listOf(mockSqlDatabase)
        ))

        val model = DatabasePublishModel().apply {
            isDatabaseConnectionEnabled = true
            isCreatingSqlDatabase = true
            subscription = mockSubscription
            databaseName = "test-sql-database-1"
            isCreatingResourceGroup = false
            resourceGroupName = "test-resource-group"
            isCreatingSqlServer = true
            sqlServerName = "test-sql-server"
        }

        SqlDatabaseConfigValidator.validateDatabaseConnection(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "SQL Server Admin Login is not defined\\.")
    fun testValidateDatabaseConnection_SqlServerAdminLoginIsNotSet() {
        val mockSubscription = SubscriptionMock()
        val mockSqlServer = SqlServerMock(name = "test-sql-server")
        AzureSqlServerMvpModel.setSubscriptionIdToSqlServersMap(mapOf(
                mockSubscription.subscriptionId() to listOf(mockSqlServer)
        ))

        val mockSqlDatabase = SqlDatabaseMock(name = "test-sql-database")
        AzureSqlDatabaseMvpModel.setSqlServerToSqlDatabasesMap(mapOf(
                mockSqlServer to listOf(mockSqlDatabase)
        ))

        val model = DatabasePublishModel().apply {
            isDatabaseConnectionEnabled = true
            isCreatingSqlDatabase = true
            subscription = mockSubscription
            databaseName = "test-sql-database-1"
            isCreatingResourceGroup = false
            resourceGroupName = "test-resource-group"
            isCreatingSqlServer = true
            sqlServerName = "test-sql-server-1"
            sqlServerAdminLogin = ""
        }

        SqlDatabaseConfigValidator.validateDatabaseConnection(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "SQL Server Admin Password is not defined\\.")
    fun testValidateDatabaseConnection_SqlServerAdminPasswordIsNotSet() {
        val mockSubscription = SubscriptionMock()
        val mockSqlServer = SqlServerMock(name = "test-sql-server")
        AzureSqlServerMvpModel.setSubscriptionIdToSqlServersMap(mapOf(
                mockSubscription.subscriptionId() to listOf(mockSqlServer)
        ))

        val mockSqlDatabase = SqlDatabaseMock(name = "test-sql-database")
        AzureSqlDatabaseMvpModel.setSqlServerToSqlDatabasesMap(mapOf(
                mockSqlServer to listOf(mockSqlDatabase)
        ))

        val model = DatabasePublishModel().apply {
            isDatabaseConnectionEnabled = true
            isCreatingSqlDatabase = true
            subscription = mockSubscription
            databaseName = "test-sql-database-1"
            isCreatingResourceGroup = false
            resourceGroupName = "test-resource-group"
            isCreatingSqlServer = true
            sqlServerName = "test-sql-server-1"
            sqlServerAdminLogin = "user"
            sqlServerAdminPassword = CharArray(0)
        }

        SqlDatabaseConfigValidator.validateDatabaseConnection(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Passwords do not match\\.")
    fun testValidateDatabaseConnection_SqlServerAdminPasswordDoNotMatch() {
        val mockSubscription = SubscriptionMock()
        val mockSqlServer = SqlServerMock(name = "test-sql-server")
        AzureSqlServerMvpModel.setSubscriptionIdToSqlServersMap(mapOf(
                mockSubscription.subscriptionId() to listOf(mockSqlServer)
        ))

        val mockSqlDatabase = SqlDatabaseMock(name = "test-sql-database")
        AzureSqlDatabaseMvpModel.setSqlServerToSqlDatabasesMap(mapOf(
                mockSqlServer to listOf(mockSqlDatabase)
        ))

        val model = DatabasePublishModel().apply {
            isDatabaseConnectionEnabled = true
            isCreatingSqlDatabase = true
            subscription = mockSubscription
            databaseName = "test-sql-database-1"
            isCreatingResourceGroup = false
            resourceGroupName = "test-resource-group"
            isCreatingSqlServer = true
            sqlServerName = "test-sql-server-1"
            sqlServerAdminLogin = "user"
            sqlServerAdminPassword = "Pa#sw0rd".toCharArray()
            sqlServerAdminPasswordConfirm = "".toCharArray()
        }

        SqlDatabaseConfigValidator.validateDatabaseConnection(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "SQL Database Collation is not provided\\.")
    fun testValidateDatabaseConnection_CollationIsNotSet() {
        val mockSubscription = SubscriptionMock()
        val mockSqlServer = SqlServerMock(name = "test-sql-server")
        AzureSqlServerMvpModel.setSubscriptionIdToSqlServersMap(mapOf(
                mockSubscription.subscriptionId() to listOf(mockSqlServer)
        ))

        val mockSqlDatabase = SqlDatabaseMock(name = "test-sql-database")
        AzureSqlDatabaseMvpModel.setSqlServerToSqlDatabasesMap(mapOf(
                mockSqlServer to listOf(mockSqlDatabase)
        ))

        val model = DatabasePublishModel().apply {
            isDatabaseConnectionEnabled = true
            isCreatingSqlDatabase = true
            subscription = mockSubscription
            databaseName = "test-sql-database-1"
            isCreatingResourceGroup = false
            resourceGroupName = "test-resource-group"
            isCreatingSqlServer = false
            sqlServerId = "test-sql-server-id"
            sqlServerAdminPassword = "Pa#sw0rd".toCharArray()
            collation = ""
        }

        SqlDatabaseConfigValidator.validateDatabaseConnection(model)
    }

    @Test
    fun testValidateDatabaseConnection_IsSet() {
        val mockSubscription = SubscriptionMock()
        val mockSqlServer = SqlServerMock(name = "test-sql-server")
        AzureSqlServerMvpModel.setSubscriptionIdToSqlServersMap(mapOf(
                mockSubscription.subscriptionId() to listOf(mockSqlServer)
        ))

        val mockSqlDatabase = SqlDatabaseMock(name = "test-sql-database")
        AzureSqlDatabaseMvpModel.setSqlServerToSqlDatabasesMap(mapOf(
                mockSqlServer to listOf(mockSqlDatabase)
        ))

        val model = DatabasePublishModel().apply {
            isDatabaseConnectionEnabled = true
            isCreatingSqlDatabase = true
            subscription = mockSubscription
            databaseName = "test-sql-database-1"
            isCreatingResourceGroup = false
            resourceGroupName = "test-resource-group"
            isCreatingSqlServer = true
            sqlServerName = "test-sql-server-1"
            sqlServerAdminLogin = "user"
            sqlServerAdminPassword = "Pa#sw0rd".toCharArray()
            sqlServerAdminPasswordConfirm = "Pa#sw0rd".toCharArray()
            collation = "SQL_Latin1_General_CP1_CI_AS"
        }

        SqlDatabaseConfigValidator.validateDatabaseConnection(model)
    }

    //endregion New Db

    //region Existing Db

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "SQL Database is not set\\.")
    fun testValidateDatabaseConnection_ExistingDb_IdIsNotSet() {
        val model = DatabasePublishModel().apply {
            isDatabaseConnectionEnabled = true
            isCreatingSqlDatabase = false
            databaseId = ""
        }

        SqlDatabaseConfigValidator.validateDatabaseConnection(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "SQL Server Admin Login is not defined\\.")
    fun testValidateDatabaseConnection_ExistingDb_AdminLoginIsNotSet() {
        val model = DatabasePublishModel().apply {
            isDatabaseConnectionEnabled = true
            isCreatingSqlDatabase = false
            databaseId = "test-sql-database"
            sqlServerAdminLogin = ""
        }

        SqlDatabaseConfigValidator.validateDatabaseConnection(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "SQL Server Admin Password is not defined\\.")
    fun testValidateDatabaseConnection_ExistingDb_AdminPasswordIsNotSet() {
        val model = DatabasePublishModel().apply {
            isDatabaseConnectionEnabled = true
            isCreatingSqlDatabase = false
            databaseId = "test-sql-database"
            sqlServerAdminLogin = "user"
            sqlServerAdminPassword = CharArray(0)
        }

        SqlDatabaseConfigValidator.validateDatabaseConnection(model)
    }

    @Test
    fun testValidateDatabaseConnection_ExistingDb_IsSet() {
        val model = DatabasePublishModel().apply {
            isDatabaseConnectionEnabled = true
            isCreatingSqlDatabase = false
            databaseId = "test-sql-database"
            sqlServerAdminLogin = "user"
            sqlServerAdminPassword = "Pa#sw0rd".toCharArray()
        }

        SqlDatabaseConfigValidator.validateDatabaseConnection(model)
    }

    //endregion Existing Db
}
