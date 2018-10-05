package com.microsoft.intellij.runner.webapp.webappconfig.validator

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.intellij.runner.db.AzureDatabaseMvpModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppSettingModel
import com.microsoft.intellij.runner.webapp.webappconfig.UiConstants

object SqlDatabaseValidator : ConfigurationValidator() {

    private val sqlDatabaseNameRegex = "[\\s]".toRegex()

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
    fun validateDatabaseConnection(model: AzureDotNetWebAppSettingModel.DatabaseModel) {
        if (!model.isDatabaseConnectionEnabled) return

        if (model.isCreatingSqlDatabase) {
            val subscriptionId = SubscriptionValidator.validateSubscription(model.subscription).subscriptionId()
            validateDatabaseName(subscriptionId, model.databaseName, model.sqlServerName)

            if (model.isCreatingDbResourceGroup) {
                ResourceGroupValidator.validateResourceGroupName(subscriptionId, model.dbResourceGroupName)
            } else {
                checkValueIsSet(model.dbResourceGroupName, UiConstants.RESOURCE_GROUP_NAME_NOT_DEFINED)
            }

            if (model.isCreatingSqlServer) {
                validateSqlServerName(subscriptionId, model.sqlServerName)
                validateAdminLogin(model.sqlServerAdminLogin)
                validateAdminPassword(model.sqlServerAdminLogin, model.sqlServerAdminPassword)
                checkPasswordsMatch(model.sqlServerAdminPassword, model.sqlServerAdminPasswordConfirm)
            } else {
                checkValueIsSet(model.sqlServerAdminPassword, UiConstants.SQL_SERVER_ADMIN_PASSWORD_NOT_DEFINED)
                checkValueIsSet(model.sqlServerId, UiConstants.SQL_SERVER_ID_NOT_DEFINED)
            }

            validateCollation(model.collation)
        } else {
            validateDatabase(model.database)

            checkValueIsSet(model.sqlServerAdminLogin, UiConstants.SQL_SERVER_ADMIN_LOGIN_NOT_DEFINED)
            checkValueIsSet(model.sqlServerAdminPassword, UiConstants.SQL_SERVER_ADMIN_PASSWORD_NOT_DEFINED)
        }
    }

    @Throws(RuntimeConfigurationError::class)
    private fun validateDatabase(database: SqlDatabase?) {
        if (database == null) throw RuntimeConfigurationError(UiConstants.SQL_DATABASE_NOT_DEFINED)
    }

    /**
     * Validate SQL Database name
     *
     * Note: There are no any specific rules to validate SQL Database name
     *       Azure allows to create SQL Database with any name I've tested
     */
    @Throws(RuntimeConfigurationError::class)
    private fun validateDatabaseName(subscriptionId: String, name: String, sqlServerName: String) {
        checkValueIsSet(name, UiConstants.SQL_DATABASE_NAME_NOT_DEFINED)

        val matches = sqlDatabaseNameRegex.findAll(name)
        if (matches.count() > 0) {
            val invalidChars = matches.map { it.value }.distinct().joinToString("', '", "'", "'")
            throw RuntimeConfigurationError(String.format(UiConstants.SQL_DATABASE_NAME_INVALID, invalidChars))
        }

        val sqlServer = AzureDatabaseMvpModel.getSqlServerByName(subscriptionId, sqlServerName) ?: return
        checkSqlDatabaseNameExistence(name, sqlServer)
    }

    @Throws(RuntimeConfigurationError::class)
    private fun checkSqlDatabaseNameExistence(name: String, sqlServer: SqlServer) {
        if (AzureDatabaseMvpModel.listSqlDatabasesBySqlServer(sqlServer).any { it.name() == name })
            throw RuntimeConfigurationError(String.format(UiConstants.SQL_DATABASE_NAME_ALREADY_EXISTS, name))
    }

    @Throws(RuntimeConfigurationError::class)
    private fun validateSqlServerName(subscriptionId: String, name: String) {

        checkValueIsSet(name, UiConstants.SQL_SERVER_NAME_NOT_DEFINED)
        checkSqlServerExistence(subscriptionId, name)

        if (name.startsWith('-') || name.endsWith('-'))
            throw RuntimeConfigurationError(UiConstants.SQL_SERVER_NAME_CANNOT_START_END_WITH_DASH)

        validateResourceName(name,
                SQL_SERVER_NAME_MIN_LENGTH,
                SQL_SERVER_NAME_MAX_LENGTH,
                SQL_SERVER_NAME_LENGTH_ERROR,
                sqlServerNameRegex,
                UiConstants.SQL_SERVER_NAME_INVALID)
    }

    @Throws(RuntimeConfigurationError::class)
    private fun checkSqlServerExistence(subscriptionId: String, name: String) {
        if (AzureDatabaseMvpModel.getSqlServerByName(subscriptionId, name) != null)
            throw RuntimeConfigurationError(String.format(UiConstants.SQL_SERVER_NAME_ALREADY_EXISTS, name))
    }

    @Throws(RuntimeConfigurationError::class)
    private fun validateAdminLogin(username: String) {
        checkValueIsSet(username, UiConstants.SQL_SERVER_ADMIN_LOGIN_NOT_DEFINED)

        if (sqlServerRestrictedAdminLoginNames.contains(username))
            throw RuntimeConfigurationError(String.format(
                    UiConstants.SQL_SERVER_ADMIN_LOGIN_FROM_RESTRICTED_LIST,
                    username,
                    sqlServerRestrictedAdminLoginNames.joinToString("', '", "'", "'")))

        if (username.contains(adminLoginWhitespaceRegex))
            throw RuntimeConfigurationError(UiConstants.SQL_SERVER_ADMIN_LOGIN_CANNOT_CONTAIN_WHITESPACES)

        if (username.contains(adminLoginStartWithDigitNonWordRegex))
            throw RuntimeConfigurationError(UiConstants.SQL_SERVER_ADMIN_LOGIN_CANNOT_BEGIN_WITH_DIGIT_NONWORD)

        validateResourceNameRegex(username, adminLoginRegex, UiConstants.SQL_SERVER_ADMIN_LOGIN_INVALID)
    }

    /**
     * Validate a SQL Server Admin Password according to Azure rules
     *
     * @param username SQL Server admin login
     * @param password original password to validate
     */
    @Throws(RuntimeConfigurationError::class)
    private fun validateAdminPassword(username: String, password: CharArray) {
        checkValueIsSet(password, UiConstants.SQL_SERVER_ADMIN_PASSWORD_NOT_DEFINED)

        val passwordString = password.joinToString("")
        if (passwordString.contains(username)) throw RuntimeConfigurationError(UiConstants.SQL_SERVER_ADMIN_PASSWORD_CANNOT_CONTAIN_PART_OF_LOGIN)

        if (password.size < ADMIN_PASSWORD_MIN_LENGTH || password.size > ADMIN_PASSWORD_MAX_LENGTH)
            throw RuntimeConfigurationError(ADMIN_PASSWORD_LENGTH_ERROR)

        var passCategoriesCount = 0
        if (adminPasswordLowerCaseRegex.containsMatchIn(passwordString)) passCategoriesCount++
        if (adminPasswordUpperCaseRegex.containsMatchIn(passwordString)) passCategoriesCount++
        if (adminPasswordDigitRegex.containsMatchIn(passwordString)) passCategoriesCount++
        if (adminPasswordNonAlphaNumericRegex.containsMatchIn(passwordString)) passCategoriesCount++

        if (passCategoriesCount < 3) throw RuntimeConfigurationError(UiConstants.SQL_SERVER_ADMIN_PASSWORD_CATEGORY_CHECK_FAILED)
    }

    /**
     * Validate if password matches the confirmation password value
     */
    @Throws(RuntimeConfigurationError::class)
    private fun checkPasswordsMatch(password: CharArray, confirmPassword: CharArray) {
        if (!password.contentEquals(confirmPassword))
            throw RuntimeConfigurationError(UiConstants.SQL_SERVER_ADMIN_PASSWORD_DOES_NOT_MATCH)
    }

    /**
     * Validate SQL Database Collation setting
     */
    @Throws(RuntimeConfigurationError::class)
    private fun validateCollation(collation: String) {
        checkValueIsSet(collation, UiConstants.SQL_DATABASE_COLLATION_NOT_DEFINED)
    }

    @Throws(RuntimeConfigurationError::class)
    private fun checkValueIsSet(value: CharArray, message: String) {
        if (value.isEmpty()) throw RuntimeConfigurationError(message)
    }
}