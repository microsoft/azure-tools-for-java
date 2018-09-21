package com.microsoft.intellij.helpers.validator

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.microsoft.azure.management.resources.Subscription

//import com.microsoft.azuretools.core.mvp.model.database.AzureDatabaseMvpModel

object SqlServerValidator : ConfigurationValidator() {

    const val SQL_SERVER_ADMIN_LOGIN_CANNOT_BEGIN_WITH_DIGIT_NONWORD = "SQL Server Admin login must not begin with numbers or symbols"
    const val SQL_SERVER_ADMIN_LOGIN_CANNOT_CONTAIN_WHITESPACES = "SQL Server Admin login cannot contain whitespaces"
    const val SQL_SERVER_ADMIN_LOGIN_FROM_RESTRICTED_LIST = "SQL Server Admin login '%s' is from list of restricted SQL Admin names: %s"
    const val SQL_SERVER_ADMIN_LOGIN_INVALID = "SQL Server Admin login should not unicode characters, or nonalphabetic characters."
    const val SQL_SERVER_ADMIN_LOGIN_NOT_DEFINED = "SQL Server Admin Login is not defined"
    const val SQL_SERVER_ADMIN_PASSWORD_CANNOT_CONTAIN_PART_OF_LOGIN =
            "Your password cannot contain all or part of the login name. Part of a login name is defined as three or more consecutive alphanumeric characters."
    const val SQL_SERVER_ADMIN_PASSWORD_CATEGORY_CHECK_FAILED =
            "Your password must contain characters from three of the following categories – English uppercase letters, " +
                    "English lowercase letters, numbers (0-9), and non-alphanumeric characters (!, \$, #, %, etc.)."
    const val SQL_SERVER_ADMIN_PASSWORD_DOES_NOT_MATCH = "Passwords do not match"
    const val SQL_SERVER_ADMIN_PASSWORD_NOT_DEFINED = "SQL Server Admin Password is not defined"
    const val SQL_SERVER_NAME_ALREADY_EXISTS = "SQL Server name '%s' already exists"
    const val SQL_SERVER_NAME_CANNOT_START_END_WITH_DASH = "SQL Server name cannot begin or end with '-' symbol"
    const val SQL_SERVER_NAME_INVALID = "SQL Server name cannot contain characters: %s"
    const val SQL_SERVER_NAME_NOT_DEFINED = "SQL Server Name is not defined"

    private val sqlServerNameRegex = "[^a-z0-9-]".toRegex()
    private const val SQL_SERVER_NAME_MIN_LENGTH = 1
    private const val SQL_SERVER_NAME_MAX_LENGTH = 63
    private const val SQL_SERVER_NAME_LENGTH_ERROR =
            "SQL Server name should be from $SQL_SERVER_NAME_MIN_LENGTH to $SQL_SERVER_NAME_MAX_LENGTH characters"

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
            "Your password must be from $ADMIN_PASSWORD_MIN_LENGTH to $ADMIN_PASSWORD_MAX_LENGTH characters"

    @Throws(RuntimeConfigurationError::class)
    fun validateSqlServer(subscription: Subscription?,
                          isCreatingResourceGroup: Boolean,
                          resourceGroupName: String,
                          sqlServerName: String,
                          sqlServerAdminLogin: String,
                          sqlServerAdminPassword: CharArray,
                          sqlServerAdminPasswordConfirm: CharArray) {

        val subscriptionId = SubscriptionValidator.validateSubscription(subscription).subscriptionId()

        if (isCreatingResourceGroup) {
            ResourceGroupValidator.validateResourceGroupName(subscriptionId, resourceGroupName)
        } else {
            checkValueIsSet(resourceGroupName, ResourceGroupValidator.RESOURCE_GROUP_NAME_NOT_DEFINED)
        }

        validateSqlServerName(subscriptionId, sqlServerName)
        validateAdminLogin(sqlServerAdminLogin)
        validateAdminPassword(sqlServerAdminLogin, sqlServerAdminPassword)
        checkPasswordsMatch(sqlServerAdminPassword, sqlServerAdminPasswordConfirm)
    }

    @Throws(RuntimeConfigurationError::class)
    private fun validateSqlServerName(subscriptionId: String, name: String) {

        checkValueIsSet(name, SQL_SERVER_NAME_NOT_DEFINED)
        checkSqlServerExistence(subscriptionId, name)

        if (name.startsWith('-') || name.endsWith('-'))
            throw RuntimeConfigurationError(SQL_SERVER_NAME_CANNOT_START_END_WITH_DASH)

        validateResourceName(name,
                SQL_SERVER_NAME_MIN_LENGTH,
                SQL_SERVER_NAME_MAX_LENGTH,
                SQL_SERVER_NAME_LENGTH_ERROR,
                sqlServerNameRegex,
                SQL_SERVER_NAME_INVALID)
    }

    @Throws(RuntimeConfigurationError::class)
    private fun checkSqlServerExistence(subscriptionId: String, name: String) {
//        if (AzureDatabaseMvpModel.getSqlServerByName(subscriptionId, name) != null)
//            throw RuntimeConfigurationError(String.format(SQL_SERVER_NAME_ALREADY_EXISTS, name))
    }

    @Throws(RuntimeConfigurationError::class)
    private fun validateAdminLogin(username: String) {
        checkValueIsSet(username, SQL_SERVER_ADMIN_LOGIN_NOT_DEFINED)

        if (sqlServerRestrictedAdminLoginNames.contains(username))
            throw RuntimeConfigurationError(String.format(
                    SQL_SERVER_ADMIN_LOGIN_FROM_RESTRICTED_LIST,
                    username,
                    sqlServerRestrictedAdminLoginNames.joinToString("', '", "'", "'")))

        if (username.contains(adminLoginWhitespaceRegex))
            throw RuntimeConfigurationError(SQL_SERVER_ADMIN_LOGIN_CANNOT_CONTAIN_WHITESPACES)

        if (username.contains(adminLoginStartWithDigitNonWordRegex))
            throw RuntimeConfigurationError(SQL_SERVER_ADMIN_LOGIN_CANNOT_BEGIN_WITH_DIGIT_NONWORD)

        validateResourceNameRegex(username, adminLoginRegex, SQL_SERVER_ADMIN_LOGIN_INVALID)
    }

    /**
     * Validate a SQL Server Admin Password according to Azure rules
     *
     * @param username SQL Server admin login
     * @param password original password to validate
     */
    @Throws(RuntimeConfigurationError::class)
    private fun validateAdminPassword(username: String, password: CharArray) {
        checkValueIsSet(password, SQL_SERVER_ADMIN_PASSWORD_NOT_DEFINED)

        val passwordString = password.joinToString("")
        if (passwordString.contains(username)) throw RuntimeConfigurationError(SQL_SERVER_ADMIN_PASSWORD_CANNOT_CONTAIN_PART_OF_LOGIN)

        if (password.size < ADMIN_PASSWORD_MIN_LENGTH || password.size > ADMIN_PASSWORD_MAX_LENGTH)
            throw RuntimeConfigurationError(ADMIN_PASSWORD_LENGTH_ERROR)

        var passCategoriesCount = 0
        if (adminPasswordLowerCaseRegex.containsMatchIn(passwordString)) passCategoriesCount++
        if (adminPasswordUpperCaseRegex.containsMatchIn(passwordString)) passCategoriesCount++
        if (adminPasswordDigitRegex.containsMatchIn(passwordString)) passCategoriesCount++
        if (adminPasswordNonAlphaNumericRegex.containsMatchIn(passwordString)) passCategoriesCount++

        if (passCategoriesCount < 3) throw RuntimeConfigurationError(SQL_SERVER_ADMIN_PASSWORD_CATEGORY_CHECK_FAILED)
    }

    /**
     * Validate if password matches the confirmation password value
     */
    @Throws(RuntimeConfigurationError::class)
    private fun checkPasswordsMatch(password: CharArray, confirmPassword: CharArray) {
        if (!password.contentEquals(confirmPassword))
            throw RuntimeConfigurationError(SQL_SERVER_ADMIN_PASSWORD_DOES_NOT_MATCH)
    }
}