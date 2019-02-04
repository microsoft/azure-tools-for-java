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

import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel

object SqlServerValidator : AzureResourceValidator() {

    private val sqlServerNameRegex = "[^a-z0-9-]".toRegex()

    private const val SQL_SERVER_ID_NOT_DEFINED = "SQL Server ID is not defined."

    private const val SQL_SERVER_NOT_DEFINED = "SQL Server is not defined."
    private const val SQL_SERVER_NAME_NOT_DEFINED = "SQL Server Name is not defined."
    private const val SQL_SERVER_NAME_CANNOT_START_END_WITH_DASH = "SQL Server name cannot begin or end with '-' symbol."
    private const val SQL_SERVER_NAME_INVALID = "SQL Server name cannot contain characters: %s."
    private const val SQL_SERVER_NAME_ALREADY_EXISTS = "SQL Server name '%s' already exists."
    private const val SQL_SERVER_NAME_MIN_LENGTH = 1
    private const val SQL_SERVER_NAME_MAX_LENGTH = 63
    private const val SQL_SERVER_NAME_LENGTH_ERROR =
            "SQL Server name should be from $SQL_SERVER_NAME_MIN_LENGTH to $SQL_SERVER_NAME_MAX_LENGTH characters."

    private const val SQL_SERVER_ADMIN_LOGIN_NOT_DEFINED = "SQL Server Admin Login is not defined."
    private const val SQL_SERVER_ADMIN_LOGIN_FROM_RESTRICTED_LIST =
            "SQL Server Admin login '%s' is from list of restricted SQL Admin names: %s."
    private const val SQL_SERVER_ADMIN_LOGIN_CANNOT_CONTAIN_WHITESPACES =
            "SQL Server Admin login cannot contain whitespaces."
    private const val SQL_SERVER_ADMIN_LOGIN_CANNOT_BEGIN_WITH_DIGIT_NONWORD =
            "SQL Server Admin login must not begin with numbers or symbols."
    private const val SQL_SERVER_ADMIN_LOGIN_INVALID =
            "SQL Server Admin login should not unicode characters, or nonalphabetic characters."
    private const val SQL_SERVER_ADMIN_PASSWORD_CANNOT_CONTAIN_PART_OF_LOGIN =
            "Your password cannot contain all or part of the login name. Part of a login name is defined as three or more consecutive alphanumeric characters."
    private const val SQL_SERVER_ADMIN_PASSWORD_NOT_DEFINED = "SQL Server Admin Password is not defined."
    private const val SQL_SERVER_ADMIN_PASSWORD_CATEGORY_CHECK_FAILED =
            "Your password must contain characters from three of the following categories – English uppercase letters, " +
                    "English lowercase letters, numbers (0-9), and non-alphanumeric characters (!, \$, #, %, etc.)."
    private const val SQL_SERVER_ADMIN_PASSWORD_DOES_NOT_MATCH = "Passwords do not match."

    private val adminLoginWhitespaceRegex = "\\s".toRegex()
    private val adminLoginStartWithDigitNonWordRegex = "^(\\d|\\W)".toRegex()
    private val adminLoginRegex = "[^\\p{L}0-9]".toRegex()
    private val sqlServerRestrictedAdminLoginNames =
            arrayOf("admin", "administrator", "sa", "root", "dbmanager", "loginmanager", "dbo", "guest", "public")

    // Note: this is not an inverse regex like others and must be validated accordingly
    private val adminPasswordLowerCaseRegex = "[a-z]".toRegex()
    private val adminPasswordUpperCaseRegex = "[A-Z]".toRegex()
    private val adminPasswordDigitRegex = "[0-9]".toRegex()
    private val adminPasswordNonAlphaNumericRegex = "[\\W]".toRegex()
    private const val ADMIN_PASSWORD_MIN_LENGTH = 8
    private const val ADMIN_PASSWORD_MAX_LENGTH = 128
    private const val ADMIN_PASSWORD_LENGTH_ERROR =
            "Your password must be from $ADMIN_PASSWORD_MIN_LENGTH to $ADMIN_PASSWORD_MAX_LENGTH characters."

    fun validateSqlServerName(name: String): ValidationResult {

        val status = checkSqlServerNameIsSet(name)
        if (!status.isValid) return status

        return status
                .merge(checkStartsEndsWithDash(name))
                .merge(checkNameMinLength(name))
                .merge(checkNameMaxLength(name))
                .merge(checkInvalidCharacters(name))
    }

    fun checkSqlServerIsSet(sqlServer: SqlServer?) =
            checkValueIsSet(sqlServer, SQL_SERVER_NOT_DEFINED)

    fun checkSqlServerNameIsSet(name: String) =
            checkValueIsSet(name, SQL_SERVER_NAME_NOT_DEFINED)

    fun checkSqlServerIdIsSet(sqlServerId: String?) =
            checkValueIsSet(sqlServerId, SQL_SERVER_ID_NOT_DEFINED)

    fun checkInvalidCharacters(name: String) =
            validateResourceNameRegex(name, sqlServerNameRegex, SQL_SERVER_NAME_INVALID)

    fun checkStartsEndsWithDash(name: String): ValidationResult {
        val status = ValidationResult()
        if (name.startsWith('-') || name.endsWith('-')) status.setInvalid(SQL_SERVER_NAME_CANNOT_START_END_WITH_DASH)
        return status
    }

    fun checkNameMinLength(name: String) =
            checkNameMinLength(name, SQL_SERVER_NAME_MIN_LENGTH, SQL_SERVER_NAME_LENGTH_ERROR)

    fun checkNameMaxLength(name: String) =
            checkNameMaxLength(name, SQL_SERVER_NAME_MAX_LENGTH, SQL_SERVER_NAME_LENGTH_ERROR)

    fun checkSqlServerExistence(subscriptionId: String, name: String): ValidationResult {
        val status = ValidationResult()
        if (isSqlServerExist(subscriptionId, name))
            return status.setInvalid(String.format(SQL_SERVER_NAME_ALREADY_EXISTS, name))
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
            checkValueIsSet(username, SQL_SERVER_ADMIN_LOGIN_NOT_DEFINED)

    fun checkRestrictedLogins(username: String): ValidationResult {
        val status = ValidationResult()
        if (sqlServerRestrictedAdminLoginNames.contains(username))
            return status.setInvalid(String.format(
                    SQL_SERVER_ADMIN_LOGIN_FROM_RESTRICTED_LIST,
                    username,
                    sqlServerRestrictedAdminLoginNames.joinToString("', '", "'", "'")))

        return status
    }

    fun checkLoginInvalidCharacters(username: String): ValidationResult {
        val status = ValidationResult()

        if (username.contains(adminLoginWhitespaceRegex))
            status.setInvalid(SQL_SERVER_ADMIN_LOGIN_CANNOT_CONTAIN_WHITESPACES)

        if (username.contains(adminLoginStartWithDigitNonWordRegex))
            status.setInvalid(SQL_SERVER_ADMIN_LOGIN_CANNOT_BEGIN_WITH_DIGIT_NONWORD)

        return status.merge(
                validateResourceNameRegex(username, adminLoginRegex, SQL_SERVER_ADMIN_LOGIN_INVALID))
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
            checkValueIsSet(password, SQL_SERVER_ADMIN_PASSWORD_NOT_DEFINED)

    fun checkPasswordContainsUsername(password: CharArray, username: String): ValidationResult {
        val status = ValidationResult()
        if (String(password).contains(username))
            return status.setInvalid(SQL_SERVER_ADMIN_PASSWORD_CANNOT_CONTAIN_PART_OF_LOGIN)

        return status
    }

    fun checkPasswordMinLength(password: CharArray) =
            checkNameMinLength(String(password), ADMIN_PASSWORD_MIN_LENGTH, ADMIN_PASSWORD_LENGTH_ERROR)

    fun checkPasswordMaxLength(password: CharArray) =
            checkNameMaxLength(String(password), ADMIN_PASSWORD_MAX_LENGTH, ADMIN_PASSWORD_LENGTH_ERROR)

    fun checkPasswordRequirements(password: CharArray): ValidationResult {
        val status = ValidationResult()

        val passwordString = String(password)
        var passCategoriesCount = 0
        if (adminPasswordLowerCaseRegex.containsMatchIn(passwordString)) passCategoriesCount++
        if (adminPasswordUpperCaseRegex.containsMatchIn(passwordString)) passCategoriesCount++
        if (adminPasswordDigitRegex.containsMatchIn(passwordString)) passCategoriesCount++
        if (adminPasswordNonAlphaNumericRegex.containsMatchIn(passwordString)) passCategoriesCount++

        if (passCategoriesCount < 3)
            status.setInvalid(SQL_SERVER_ADMIN_PASSWORD_CATEGORY_CHECK_FAILED)

        return status
    }

    fun checkPasswordsMatch(password: CharArray, confirmPassword: CharArray): ValidationResult {
        val status = ValidationResult()
        if (!password.contentEquals(confirmPassword))
            status.setInvalid(SQL_SERVER_ADMIN_PASSWORD_DOES_NOT_MATCH)
        return status
    }

    private fun isSqlServerExist(subscriptionId: String, name: String) =
            AzureSqlServerMvpModel.getSqlServerByName(subscriptionId, name) != null
}
