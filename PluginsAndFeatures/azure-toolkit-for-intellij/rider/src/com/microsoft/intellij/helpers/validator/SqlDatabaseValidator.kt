/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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
import org.jetbrains.plugins.azure.RiderAzureBundle.message

object SqlDatabaseValidator : AzureResourceValidator() {

    private val notDefinedMessage = message("run_config.publish.validation.sql_db.not_defined")

    private val sqlDatabaseNameRegex = "[\\s]".toRegex()

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
            checkValueIsSet(name, message("run_config.publish.validation.sql_db.name_not_defined"))

    fun checkDatabaseIsSet(sqlDatabase: SqlDatabase?) =
            checkValueIsSet(sqlDatabase, notDefinedMessage)

    fun checkDatabaseIdIsSet(sqlDatabaseId: String?) =
            checkValueIsSet(sqlDatabaseId, notDefinedMessage)

    fun checkInvalidCharacters(name: String) =
            validateResourceNameRegex(
                    name = name,
                    nameRegex = sqlDatabaseNameRegex,
                    nameInvalidCharsMessage = "${message("run_config.publish.validation.sql_db.name_invalid")} %s.")

    fun checkSqlDatabaseExists(subscriptionId: String,
                               databaseName: String,
                               sqlServerName: String): ValidationResult {
        val status = ValidationResult()

        val sqlServer = AzureSqlServerMvpModel.getSqlServerByName(
                subscriptionId = subscriptionId, name = sqlServerName, force = false) ?: return status

        if (isSqlDatabaseNameExist(databaseName, sqlServer))
            status.setInvalid(message("run_config.publish.validation.sql_db.name_already_exists", databaseName))

        return status
    }

    fun checkEditionIsSet(edition: DatabaseEdition?) =
            checkValueIsSet(edition, message("run_config.publish.validation.sql_db.edition_not_defined"))

    fun checkComputeSizeIsSet(objective: ServiceObjectiveName?) =
            checkValueIsSet(objective, message("run_config.publish.validation.sql_db.compute_size_not_defined"))

    fun checkCollationIsSet(collation: String?) =
            checkValueIsSet(collation, message("run_config.publish.validation.sql_db.collation_not_defined"))

    private fun isSqlDatabaseNameExist(name: String, sqlServer: SqlServer) =
            AzureSqlDatabaseMvpModel.listSqlDatabasesBySqlServer(sqlServer = sqlServer, force = false)
                    .any { sqlDatabase -> sqlDatabase.name() == name }
}
