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

package com.microsoft.intellij.helpers.validator

import com.jetbrains.rider.test.asserts.shouldBe
import com.jetbrains.rider.test.asserts.shouldBeEmpty
import com.jetbrains.rider.test.asserts.shouldBeFalse
import com.jetbrains.rider.test.asserts.shouldBeTrue
import com.microsoft.azure.management.sql.DatabaseEdition
import com.microsoft.azure.management.sql.ServiceObjectiveName
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlDatabaseMvpModel
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel
import org.jetbrains.mock.SqlDatabaseMock
import org.jetbrains.mock.SqlServerMock
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.testng.annotations.AfterMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class SqlDatabaseValidatorTest {

    @AfterMethod(alwaysRun = true)
    fun resetCacheMaps() {
        AzureSqlServerMvpModel.clearSubscriptionIdToSqlServersMap()
        AzureSqlDatabaseMvpModel.clearSqlServerToSqlDatabasesMap()
    }

    //region SQL Database

    @Test
    fun testCheckDatabaseIsSet_IsSet() {
        val validationResult = SqlDatabaseValidator.checkDatabaseIsSet(SqlDatabaseMock())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckDatabaseIsSet_IsNotSet() {
        val validationResult = SqlDatabaseValidator.checkDatabaseIsSet(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_db.not_defined"))
    }

    //endregion SQL Database

    //region ID

    @Test
    fun testCheckDatabaseIdIsSet_IsSet() {
        val validationResult = SqlDatabaseValidator.checkDatabaseIdIsSet("test-sql-database-id")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckDatabaseIdIsSet_IsNotSet() {
        val validationResult = SqlDatabaseValidator.checkDatabaseIdIsSet(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_db.not_defined"))
    }

    @Test
    fun testCheckDatabaseIdIsSet_IsEmpty() {
        val validationResult = SqlDatabaseValidator.checkDatabaseIdIsSet("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_db.not_defined"))
    }

    //endregion ID

    //region Name

    @Test
    fun testCheckDatabaseNameIsSet_IsSet() {
        val validationResult = SqlDatabaseValidator.checkDatabaseNameIsSet("test-sql-database")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckDatabaseNameIsSet_IsEmpty() {
        val validationResult = SqlDatabaseValidator.checkDatabaseNameIsSet("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_db.name_not_defined"))
    }

    //endregion Name

    //region Edition

    @Test
    fun testCheckEditionIsSet_IsSet() {
        val validationResult = SqlDatabaseValidator.checkEditionIsSet(DatabaseEdition.BASIC)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckEditionIsSet_IsNotSet() {
        val validationResult = SqlDatabaseValidator.checkEditionIsSet(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_db.edition_not_defined"))
    }

    //endregion Edition

    //region Compute Size

    @Test
    fun testCheckComputeSizeIsSet_IsSet() {
        val validationResult = SqlDatabaseValidator.checkComputeSizeIsSet(ServiceObjectiveName.BASIC)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckComputeSizeIsSet_IsNotSet() {
        val validationResult = SqlDatabaseValidator.checkComputeSizeIsSet(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_db.compute_size_not_defined"))
    }

    //endregion Compute Size

    //region Collation

    @Test
    fun testCheckCollationIsSet_IsSet() {
        val validationResult = SqlDatabaseValidator.checkCollationIsSet("Some_Default_Collation")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckCollationIsSet_IsNotSet() {
        val validationResult = SqlDatabaseValidator.checkCollationIsSet(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_db.collation_not_defined"))
    }

    @Test
    fun testCheckCollationIsSet_IsEmpty() {
        val validationResult = SqlDatabaseValidator.checkCollationIsSet("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_db.collation_not_defined"))
    }

    //endregion Collation

    //region Invalid Characters

    @DataProvider(name = "sqlDatabaseNameInvalidCharactersData")
    fun sqlDatabaseNameInvalidCharactersData() = arrayOf(
            arrayOf("Space", "sql database name", arrayOf(' '))
    )

    @Test(dataProvider = "sqlDatabaseNameInvalidCharactersData")
    fun testCheckInvalidCharacters_InvalidCharacters(name: String, sqlDatabaseName: String, invalidChars: Array<Char>) {
        val invalidCharsString = invalidChars.joinToString("', '", "'", "'")

        val validationResult = SqlDatabaseValidator.checkInvalidCharacters(sqlDatabaseName)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("${RiderAzureBundle.message("run_config.publish.validation.sql_db.name_invalid")} $invalidCharsString.")
    }

    @DataProvider(name = "sqlDatabaseNameValidCharactersData")
    fun sqlDatabaseNameValidCharactersData() = arrayOf(
            arrayOf("NonAlphabeticChar", "£∂®-sql-database-¢≥"),
            arrayOf("Symbols", "sql/database&name"),
            arrayOf("UpperCase", "SqlDatabaseName"),
            arrayOf("Numbers", "sql-database-name-123"),
            arrayOf("Underscore", "sql_database_name"),
            arrayOf("Nbsp", "sql${'\u00A0'}database${'\u00A0'}name")
    )

    @Test(dataProvider = "sqlDatabaseNameValidCharactersData")
    fun testCheckInvalidCharacters_ValidCharacters(name: String, sqlDatabaseName: String) {
        val validationResult = SqlDatabaseValidator.checkInvalidCharacters(sqlDatabaseName)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Invalid Characters

    //region Name Exists

    @Test
    fun testCheckSqlDatabaseExists_Exists() {
        val mockSqlServer = SqlServerMock(name = "test-sql-server")
        AzureSqlServerMvpModel.setSubscriptionIdToSqlServersMap(mapOf(
                "test-subscription" to listOf(mockSqlServer)
        ))

        val mockSqlDatabase = SqlDatabaseMock(name = "test-sql-database")
        AzureSqlDatabaseMvpModel.setSqlServerToSqlDatabasesMap(mapOf(
                mockSqlServer to listOf(mockSqlDatabase)
        ))

        val validationResult = SqlDatabaseValidator.checkSqlDatabaseExists(
                subscriptionId = "test-subscription", databaseName = mockSqlDatabase.name(), sqlServerName = mockSqlServer.name())

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_db.name_already_exists", mockSqlDatabase.name()))
    }

    @Test
    fun testCheckSqlDatabaseExists_SqlServerNotExist() {
        val mockSqlServer = SqlServerMock(name = "test-sql-server")
        AzureSqlServerMvpModel.setSubscriptionIdToSqlServersMap(mapOf(
                "test-subscription" to listOf(mockSqlServer)
        ))

        val mockSqlDatabase = SqlDatabaseMock(name = "test-sql-database")
        AzureSqlDatabaseMvpModel.setSqlServerToSqlDatabasesMap(mapOf(
                mockSqlServer to listOf(mockSqlDatabase)
        ))

        val validationResult = SqlDatabaseValidator.checkSqlDatabaseExists(
                subscriptionId = "test-subscription", databaseName = mockSqlDatabase.name(), sqlServerName = "test-sql-server-1")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckSqlDatabaseExists_NotExist() {
        val mockSqlServer = SqlServerMock(name = "test-sql-server")
        AzureSqlServerMvpModel.setSubscriptionIdToSqlServersMap(mapOf(
                "test-subscription" to listOf(mockSqlServer)
        ))

        val mockSqlDatabase = SqlDatabaseMock(name = "test-sql-database")
        AzureSqlDatabaseMvpModel.setSqlServerToSqlDatabasesMap(mapOf(
                mockSqlServer to listOf(mockSqlDatabase)
        ))

        val validationResult = SqlDatabaseValidator.checkSqlDatabaseExists(
                subscriptionId = "test-subscription", databaseName = "test-sql-database-1", sqlServerName = mockSqlServer.name())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Name Exists

    //region Validation

    @Test
    fun testValidateDatabaseName_ValidName() {
        val validationResult = SqlDatabaseValidator.validateDatabaseName("test-sql-database")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testValidateDatabaseName_NameIsNotSet() {
        val validationResult = SqlDatabaseValidator.validateDatabaseName("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_db.name_not_defined"))
    }

    @Test
    fun testValidateDatabaseName_MultipleErrors() {
        val invalidCharsString = arrayOf(' ').joinToString("', '", "'", "'")

        val validationResult = SqlDatabaseValidator.validateDatabaseName("test sql database")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("${RiderAzureBundle.message("run_config.publish.validation.sql_db.name_invalid")} $invalidCharsString.")
    }

    //endregion Validation
}
