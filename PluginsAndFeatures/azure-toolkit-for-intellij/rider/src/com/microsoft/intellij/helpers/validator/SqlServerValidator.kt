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

import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel
import org.jetbrains.plugins.azure.RiderAzureBundle.message

object SqlServerValidator : AzureResourceValidator() {

    private const val SQL_SERVER_NAME_MIN_LENGTH = 1
    private const val SQL_SERVER_NAME_MAX_LENGTH = 63

    private const val ADMIN_PASSWORD_MIN_LENGTH = 8
    private const val ADMIN_PASSWORD_MAX_LENGTH = 128

    private val sqlServerNameRegex = "[^a-z0-9-]".toRegex()

    private val nameLengthError =
            message("run_config.publish.validation.sql_server.name_length_error",
                    SQL_SERVER_NAME_MIN_LENGTH, SQL_SERVER_NAME_MAX_LENGTH)

    private val passwordLengthError =
            message("run_config.publish.validation.sql_server.admin_password_length_error",
                    ADMIN_PASSWORD_MIN_LENGTH, ADMIN_PASSWORD_MAX_LENGTH)

    private val adminLoginStartWithDigitNonWordRegex = "^(\\d|\\W)".toRegex()
    private val adminLoginRegex = "[^\\p{L}0-9]".toRegex()
    private val sqlServerRestrictedAdminLoginNames =
            arrayOf("admin", "administrator", "sa", "root", "dbmanager", "loginmanager", "dbo", "guest", "public")

    // Note: this is not an inverse regex like others and must be validated accordingly
    private val adminPasswordLowerCaseRegex = "[a-z]".toRegex()
    private val adminPasswordUpperCaseRegex = "[A-Z]".toRegex()
    private val adminPasswordDigitRegex = "[0-9]".toRegex()
    private val adminPasswordNonAlphaNumericRegex = "[\\W]".toRegex()

    fun validateSqlServerName(name: String): ValidationResult {

        val status = checkSqlServerNameIsSet(name)
        if (!status.isValid) return status

        return status
                .merge(checkStartsEndsWithDash(name))
                .merge(checkSqlServerNameMinLength(name))
                .merge(checkSqlServerNameMaxLength(name))
                .merge(checkInvalidCharacters(name))
    }

    fun checkSqlServerIsSet(sqlServer: SqlServer?) =
            checkValueIsSet(sqlServer, message("run_config.publish.validation.sql_server.not_defined"))

    fun checkSqlServerNameIsSet(name: String) =
            checkValueIsSet(name, message("run_config.publish.validation.sql_server.name_not_defined"))

    fun checkSqlServerIdIsSet(sqlServerId: String?) =
            checkValueIsSet(sqlServerId, message("run_config.publish.validation.sql_server.id_not_defined"))

    fun checkInvalidCharacters(name: String) =
            validateResourceNameRegex(
                    name = name,
                    nameRegex = sqlServerNameRegex,
                    nameInvalidCharsMessage = "${message("run_config.publish.validation.sql_server.name_invalid")} %s.")

    fun checkStartsEndsWithDash(name: String): ValidationResult {
        val status = ValidationResult()
        if (name.startsWith('-') || name.endsWith('-'))
            status.setInvalid(message("run_config.publish.validation.sql_server.name_cannot_start_end_with_dash"))

        return status
    }

    fun checkSqlServerNameMinLength(name: String) =
            checkNameMinLength(
                    name = name,
                    minLength = SQL_SERVER_NAME_MIN_LENGTH,
                    errorMessage = nameLengthError)

    fun checkSqlServerNameMaxLength(name: String) =
            checkNameMaxLength(
                    name = name,
                    maxLength = SQL_SERVER_NAME_MAX_LENGTH,
                    errorMessage = nameLengthError)

    fun checkSqlServerExistence(subscriptionId: String, name: String): ValidationResult {
        val status = ValidationResult()
        if (isSqlServerExist(subscriptionId, name))
            return status.setInvalid(message("run_config.publish.validation.sql_server.name_already_exists", name))

        return status
    }

    fun validateAdminLogin(username: String): ValidationResult {
        val status = checkAdminLoginIsSet(username)
        if (!status.isValid) return status

        status.merge(checkRestrictedLogins(username))
        if (!status.isValid) return status

        return status.merge(checkLoginInvalidCharacters(username))
    }

    fun checkAdminLoginIsSet(username: String?) =
            checkValueIsSet(username, message("run_config.publish.validation.sql_server.admin_login_not_defined"))

    fun checkRestrictedLogins(username: String): ValidationResult {
        val status = ValidationResult()
        if (sqlServerRestrictedAdminLoginNames.contains(username))
            return status.setInvalid(
                    message("run_config.publish.validation.sql_server.admin_login_from_restriction_list",
                            username, sqlServerRestrictedAdminLoginNames.joinToString("', '", "'", "'")))

        return status
    }

    fun checkLoginInvalidCharacters(username: String): ValidationResult {
        val status = ValidationResult()

        if (username.contains(adminLoginStartWithDigitNonWordRegex))
            status.setInvalid(message("run_config.publish.validation.sql_server.admin_login_cannot_begin_with_digit_nonword"))

        return status.merge(
                validateResourceNameRegex(
                        name = username,
                        nameRegex = adminLoginRegex,
                        nameInvalidCharsMessage = "${message("run_config.publish.validation.sql_server.admin_login_invalid")} %s."))
    }

    /**
     * Validate a SQL Server Admin Password according to Azure rules
     *
     * @param username SQL Server admin login
     * @param password original password to validate
     */
    fun validateAdminPassword(username: String, password: CharArray): ValidationResult {
        val status = checkPasswordIsSet(password)
        if (!status.isValid) return status

        status.merge(checkPasswordContainsUsername(password, username))
        if (!status.isValid) return status

        return status
                .merge(checkPasswordMinLength(password))
                .merge(checkPasswordMaxLength(password))
                .merge(checkPasswordRequirements(password))
    }

    fun checkPasswordIsSet(password: CharArray) =
            checkValueIsSet(password, message("run_config.publish.validation.sql_server.admin_password_not_defined"))

    fun checkPasswordContainsUsername(password: CharArray, username: String): ValidationResult {
        val status = ValidationResult()
        if (String(password).contains(username))
            return status.setInvalid(message("run_config.publish.validation.sql_server.admin_password_cannot_contain_part_of_login"))

        return status
    }

    fun checkPasswordMinLength(password: CharArray) =
            checkNameMinLength(
                    name = String(password),
                    minLength = ADMIN_PASSWORD_MIN_LENGTH,
                    errorMessage = passwordLengthError)

    fun checkPasswordMaxLength(password: CharArray) =
            checkNameMaxLength(
                    name = String(password),
                    maxLength = ADMIN_PASSWORD_MAX_LENGTH,
                    errorMessage = passwordLengthError)

    fun checkPasswordRequirements(password: CharArray): ValidationResult {
        val status = ValidationResult()

        val passwordString = String(password)
        var passCategoriesCount = 0
        if (adminPasswordLowerCaseRegex.containsMatchIn(passwordString)) passCategoriesCount++
        if (adminPasswordUpperCaseRegex.containsMatchIn(passwordString)) passCategoriesCount++
        if (adminPasswordDigitRegex.containsMatchIn(passwordString)) passCategoriesCount++
        if (adminPasswordNonAlphaNumericRegex.containsMatchIn(passwordString)) passCategoriesCount++

        if (passCategoriesCount < 3)
            status.setInvalid(message("run_config.publish.validation.sql_server.admin_password_category_check_failed"))

        return status
    }

    fun checkPasswordsMatch(password: CharArray, confirmPassword: CharArray): ValidationResult {
        val status = ValidationResult()
        if (!password.contentEquals(confirmPassword))
            status.setInvalid(message("run_config.publish.validation.sql_server.admin_password_do_not_match"))

        return status
    }

    private fun isSqlServerExist(subscriptionId: String, name: String) =
            AzureSqlServerMvpModel.getSqlServerByName(subscriptionId, name) != null
}
