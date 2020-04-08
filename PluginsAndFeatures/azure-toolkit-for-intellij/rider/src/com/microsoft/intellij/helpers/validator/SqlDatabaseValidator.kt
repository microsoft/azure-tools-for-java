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

package com.microsoft.intellij.helpers.validator

import com.microsoft.azure.management.sql.DatabaseEdition
import com.microsoft.azure.management.sql.ServiceObjectiveName
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlDatabaseMvpModel
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel

object SqlDatabaseValidator : AzureResourceValidator() {

    private val sqlDatabaseNameRegex = "[\\s]".toRegex()

    private const val SQL_DATABASE_NOT_DEFINED = "SQL Database is not set"

    private const val SQL_DATABASE_NAME_INVALID = "SQL Database name cannot contain characters: %s."
    private const val SQL_DATABASE_NAME_NOT_DEFINED = "SQL Database name is not defined."
    private const val SQL_DATABASE_NAME_ALREADY_EXISTS = "SQL Database name '%s' already exists."
    private const val SQL_DATABASE_EDITION_NOT_DEFINED = "SQL Database Edition is not provided."
    private const val SQL_DATABASE_COLLATION_NOT_DEFINED = "SQL Database Collation is not provided."
    private const val SQL_DATABASE_COMPUTE_SIZE_NOT_DEFINED = "SQL Database Compute Size is not provided."

    /**
     * Validate SQL Database name
     *
     * Note: There are no any specific rules to validate SQL Database name
     *       Azure allows to create SQL Database with any name I've tested
     */
    fun validateDatabaseName(name: String): ValidationResult {
        val status = checkDatabaseNameIsSet(name)
        if (!status.isValid) return status

        return checkInvalidCharacters(name)
    }

    fun checkDatabaseNameIsSet(name: String) =
            checkValueIsSet(name, SQL_DATABASE_NAME_NOT_DEFINED)

    fun checkDatabaseIsSet(sqlDatabase: SqlDatabase?) =
            checkValueIsSet(sqlDatabase, SQL_DATABASE_NOT_DEFINED)

    fun checkDatabaseIdIsSet(sqlDatabaseId: String?) =
            checkValueIsSet(sqlDatabaseId, SQL_DATABASE_NOT_DEFINED)

    fun checkInvalidCharacters(name: String) =
            validateResourceNameRegex(name, sqlDatabaseNameRegex, SQL_DATABASE_NAME_INVALID)

    fun checkSqlDatabaseExistence(subscriptionId: String,
                                  databaseName: String,
                                  sqlServerName: String): ValidationResult {
        val status = ValidationResult()

        val sqlServer = AzureSqlServerMvpModel.getSqlServerByName(subscriptionId, sqlServerName) ?: return status
        if (isSqlDatabaseNameExist(databaseName, sqlServer))
            status.setInvalid(String.format(SQL_DATABASE_NAME_ALREADY_EXISTS, databaseName))

        return status
    }

    fun checkEditionIsSet(edition: DatabaseEdition?) =
            checkValueIsSet(edition, SQL_DATABASE_EDITION_NOT_DEFINED)

    fun checkComputeSizeIsSet(objective: ServiceObjectiveName?) =
            checkValueIsSet(objective, SQL_DATABASE_COMPUTE_SIZE_NOT_DEFINED)

    fun checkCollationIsSet(collation: String?) =
            checkValueIsSet(collation, SQL_DATABASE_COLLATION_NOT_DEFINED)

    private fun isSqlDatabaseNameExist(name: String, sqlServer: SqlServer) =
            AzureSqlDatabaseMvpModel.listSqlDatabasesBySqlServer(sqlServer).any { it.name() == name }
}
