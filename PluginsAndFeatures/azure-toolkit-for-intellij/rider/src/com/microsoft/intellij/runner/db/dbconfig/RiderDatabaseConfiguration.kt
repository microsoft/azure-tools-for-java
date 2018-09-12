package com.microsoft.intellij.runner.db.dbconfig

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.rider.util.firstOrNull
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.utils.AzureModel
import com.microsoft.intellij.runner.AzureRunConfigurationBase
import com.microsoft.intellij.runner.db.AzureDatabaseMvpModel
import com.microsoft.intellij.runner.db.AzureDatabaseSettingModel
import java.io.IOException

class RiderDatabaseConfiguration(project: Project,
                                 factory: ConfigurationFactory,
                                 name: String)
    : AzureRunConfigurationBase<AzureDatabaseSettingModel>(project, factory, name) {

    companion object {

        private const val SIGN_IN_REQUIRED = "Please sign in with your Azure account"

        private const val SUBSCRIPTION_MISSING = "Subscription not provided"

        private val sqlDatabaseNameRegex = "[\\s]".toRegex()
        private const val SQL_DATABASE_NAME_MISSING = "SQL Database name not provided"
        private const val SQL_DATABASE_NAME_ALREADY_EXISTS = "SQL Database name '%s' already exists"
        private const val SQL_DATABASE_NAME_INVALID = "SQL Database name cannot contain characters: %s"

        private const val SQL_DATABASE_COLLATION_MISSING = "SQL Database Collation not provided"

        private val sqlServerNameRegex = "[^a-z0-9-]".toRegex()
        private const val SQL_SERVER_MISSING = "Please select a SQL Server"
        private const val SQL_SERVER_NAME_ALREADY_EXISTS = "SQL Server name '%s' already exists"
        private const val SQL_SERVER_NAME_MIN_LENGTH = 1
        private const val SQL_SERVER_NAME_MAX_LENGTH = 63
        private const val SQL_SERVER_NAME_MISSING = "SQL Server name not provided"
        private const val SQL_SERVER_NAME_LENGTH_ERROR =
                "SQL Server name should be from $SQL_SERVER_NAME_MIN_LENGTH to $SQL_SERVER_NAME_MAX_LENGTH characters"
        private const val SQL_SERVER_NAME_INVALID = "SQL Server name cannot contain characters: %s"
        private const val SQL_SERVER_NAME_CANNOT_START_END_WITH_DASH = "SQL Server name cannot begin or end with '-' symbol"

        private val resourceGroupNameRegex = "[^\\p{L}0-9-.()_]".toRegex()
        private const val RESOURCE_GROUP_MISSING = "Please select a Resource Group"
        private const val RESOURCE_GROUP_ALREADY_EXISTS = "Resource Group with name '%s' already exists"
        private const val RESOURCE_GROUP_NAME_MIN_LENGTH = 1
        private const val RESOURCE_GROUP_NAME_MAX_LENGTH = 90
        private const val RESOURCE_GROUP_NAME_MISSING = "Resource Group name not provided"
        private const val RESOURCE_GROUP_NAME_LENGTH_ERROR =
                "Resource Group name should be from $RESOURCE_GROUP_NAME_MIN_LENGTH to $RESOURCE_GROUP_NAME_MAX_LENGTH characters"
        private const val RESOURCE_GROUP_NAME_INVALID = "Resource Group name cannot contain characters: %s"
        private const val RESOURCE_GROUP_NAME_CANNOT_ENDS_WITH_PERIOD = "Resource Group name cannot ends with period symbol"

        private val adminLoginWhitespaceRegex = "\\s".toRegex()
        private val adminLoginStartWithDigitNonWordRegex = "^(\\d|\\W)".toRegex()
        private val adminLoginRegex = "[^\\p{L}0-9]".toRegex()
        private const val ADMIN_LOGIN_MISSING = "Administrator login not provided"
        private val sqlServerRestrictedAdminLoginNames =
                arrayOf("admin", "administrator", "sa", "root", "dbmanager", "loginmanager", "dbo", "guest", "public")
        private const val ADMIN_LOGIN_FROM_RESTRICTED_LIST =
                "SQL Server Admin login '%s' is from list of restricted SQL Admin names: %s"
        private const val ADMIN_LOGIN_CANNOT_CONTAIN_WHITESPACES = "SQL Server Admin login cannot contain whitespaces"
        private const val ADMIN_LOGIN_CANNOT_BEGIN_WITH_DIGIT_NONWORD = "SQL Server Admin login must not begin with numbers or symbols"
        private const val ADMIN_LOGIN_INVALID = "SQL Server Admin login should not unicode characters, or nonalphabetic characters."

        private const val ADMIN_PASSWORD_MISSING = "Administrator password not provided"
        // Note: this is not an inverse regex and must be validated accordingly
        private val adminPasswordLowerCaseRegex = "[a-z]".toRegex()
        private val adminPasswordUpperCaseRegex = "[A-Z]".toRegex()
        private val adminPasswordDigitRegex = "[0-9]".toRegex()
        private val adminPasswordNonAlphaNumericRegex = "[\\W]".toRegex()
        private const val ADMIN_PASSWORD_MIN_LENGTH = 8
        private const val ADMIN_PASSWORD_MAX_LENGTH = 128
        private const val ADMIN_PASSWORD_LENGTH_ERROR =
                "Your password must be from $ADMIN_PASSWORD_MIN_LENGTH to $ADMIN_PASSWORD_MAX_LENGTH characters"
        private const val ADMIN_PASSWORD_CATEGORY_CHECK_FAILED =
                "Your password must contain characters from three of the following categories – English uppercase letters, " +
                "English lowercase letters, numbers (0-9), and non-alphanumeric characters (!, \$, #, %, etc.)."
        private const val ADMIN_PASSWORD_CANNOT_CONTAIN_PART_OF_LOGIN =
                "Your password cannot contain all or part of the login name. Part of a login name is defined as three or more consecutive alphanumeric characters."
        private const val ADMIN_PASSWORD_DOES_NOT_MATCH = "Passwords do not match"
    }

    private val myModel: AzureDatabaseSettingModel = AzureDatabaseSettingModel()

    override fun getSubscriptionId(): String? {
        return myModel.subscriptionId
    }

    override fun getTargetPath() = ""
    override fun getTargetName() = myModel.databaseName

    override fun getModel(): AzureDatabaseSettingModel {
        return myModel
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return RiderDatabaseSettingEditor(project, this)
    }

    @Throws(ExecutionException::class)
    override fun getState(executor: Executor,
                          executionEnvironment: ExecutionEnvironment): RunProfileState? {
        return RiderDatabaseRunState(project, myModel)
    }

    override fun validate() { }

    /**
     * Validate the configuration to run
     *
     * @throws [RuntimeConfigurationError] when configuration miss expected fields
     */
    @Throws(RuntimeConfigurationError::class)
    override fun checkConfiguration() {

        validateAzureAccountIsSignedIn()
        validateSubscription(myModel.subscriptionId)
        validateDatabaseName(myModel.subscriptionId, myModel.databaseName, myModel.sqlServerName)

        // Resource Group
        if (myModel.isCreatingResourceGroup) {
            validateResourceGroupName(myModel.resourceGroupName)
        } else {
            checkValueIsSet(myModel.resourceGroupName, RESOURCE_GROUP_MISSING)
        }

        // SQL Server
        if (myModel.isCreatingSqlServer) {
            validateSqlServerName(myModel.subscriptionId, myModel.sqlServerName)
            validateAdminLogin(myModel.sqlServerAdminLogin)
            validateAdminPassword(myModel.sqlServerAdminLogin, myModel.sqlServerAdminPassword)
            checkPasswordsMatch(myModel.sqlServerAdminPassword, myModel.sqlServerAdminPasswordConfirm)
        } else {
            checkValueIsSet(myModel.sqlServerId, SQL_SERVER_MISSING)
        }

        validateCollation(myModel.collation)
    }

    //region Sign In

    /**
     * Check whether user is signed in to Azure account
     *
     * @throws [RuntimeConfigurationError] in case validation is failed
     */
    @Throws(RuntimeConfigurationError::class)
    private fun validateAzureAccountIsSignedIn() {
        try {
            if (!AuthMethodManager.getInstance().isSignedIn) { throw RuntimeConfigurationError(SIGN_IN_REQUIRED) }
        } catch (e: IOException) {
            throw RuntimeConfigurationError(SIGN_IN_REQUIRED)
        }
    }

    //endregion Sign In

    //region Subscription

    @Throws(RuntimeConfigurationError::class)
    private fun validateSubscription(subscriptionId: String) {
        checkValueIsSet(subscriptionId, SUBSCRIPTION_MISSING)
    }

    //endregion Subscription

    //region SQL Server

    @Throws(RuntimeConfigurationError::class)
    private fun validateSqlServerName(subscriptionId: String, name: String) {

        checkValueIsSet(name, SQL_SERVER_NAME_MISSING)
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
        if (AzureDatabaseMvpModel.getSqlServerByName(subscriptionId, name) != null)
            throw RuntimeConfigurationError(String.format(SQL_SERVER_NAME_ALREADY_EXISTS, name))
    }

    //endregion SQL Server

    //region SQL Database

    /**
     * Validate SQL Database name
     *
     * Note: There are no any specific rules to validate SQL Database name
     *       Azure allows to create SQL Database with any name I've tested
     */
    @Throws(RuntimeConfigurationError::class)
    private fun validateDatabaseName(subscriptionId: String, name: String, sqlServerName: String) {
        checkValueIsSet(name, SQL_DATABASE_NAME_MISSING)

        val matches = sqlDatabaseNameRegex.findAll(name)
        if (matches.count() > 0) {
            val invalidChars = matches.map { it.value }.distinct().joinToString("', '", "'", "'")
            throw RuntimeConfigurationError(String.format(SQL_DATABASE_NAME_INVALID, invalidChars))
        }

        val sqlServer = AzureDatabaseMvpModel.getSqlServerByName(subscriptionId, sqlServerName) ?: return
        checkSqlDatabaseNameExistence(name, sqlServer)
    }

    @Throws(RuntimeConfigurationError::class)
    private fun checkSqlDatabaseNameExistence(name: String, sqlServer: SqlServer) {
        if (AzureDatabaseMvpModel.listSqlDatabasesBySqlServer(sqlServer).any { it.name() == name })
            throw RuntimeConfigurationError(String.format(SQL_DATABASE_NAME_ALREADY_EXISTS, name))
    }

    //endregion SQL Database

    //region Resource Group

    @Throws(RuntimeConfigurationError::class)
    private fun validateResourceGroupName(name: String) {

        checkValueIsSet(name, RESOURCE_GROUP_NAME_MISSING)
        checkResourceGroupExistence(myModel.subscriptionId, name)

        if (name.endsWith('.')) throw RuntimeConfigurationError(RESOURCE_GROUP_NAME_CANNOT_ENDS_WITH_PERIOD)

        validateResourceName(name,
                RESOURCE_GROUP_NAME_MIN_LENGTH,
                RESOURCE_GROUP_NAME_MAX_LENGTH,
                RESOURCE_GROUP_NAME_LENGTH_ERROR,
                resourceGroupNameRegex,
                RESOURCE_GROUP_NAME_INVALID)
    }

    @Throws(RuntimeConfigurationError::class)
    private fun checkResourceGroupExistence(subscriptionId: String, resourceGroupName: String) {
        val subscriptionToResourceGroupMap =
                AzureModel.getInstance().subscriptionToResourceGroupMap ?: return

        val resourceGroups =
                subscriptionToResourceGroupMap.filter { it.key.subscriptionId == subscriptionId }.firstOrNull()?.value

        if (resourceGroups != null && resourceGroups.any { it.name().equals(resourceGroupName, true) })
            throw RuntimeConfigurationError(String.format(RESOURCE_GROUP_ALREADY_EXISTS, resourceGroupName))
    }

    //endregion Resource Group

    //region Admin Login/Password

    @Throws(RuntimeConfigurationError::class)
    private fun validateAdminLogin(username: String) {
        checkValueIsSet(username, ADMIN_LOGIN_MISSING)

        if (sqlServerRestrictedAdminLoginNames.contains(username))
            throw RuntimeConfigurationError(String.format(
                    ADMIN_LOGIN_FROM_RESTRICTED_LIST,
                    username,
                    sqlServerRestrictedAdminLoginNames.joinToString("', '", "'", "'")))

        if (username.contains(adminLoginWhitespaceRegex))
            throw RuntimeConfigurationError(ADMIN_LOGIN_CANNOT_CONTAIN_WHITESPACES)

        if (username.contains(adminLoginStartWithDigitNonWordRegex))
            throw RuntimeConfigurationError(ADMIN_LOGIN_CANNOT_BEGIN_WITH_DIGIT_NONWORD)

        validateResourceNameRegex(username, adminLoginRegex, ADMIN_LOGIN_INVALID)
    }

    /**
     * Validate a SQL Server Admin Password according to Azure rules
     *
     * @param username SQL Server admin login
     * @param password original password to validate
     */
    @Throws(RuntimeConfigurationError::class)
    private fun validateAdminPassword(username: String, password: CharArray) {
        if (password.isEmpty()) throw RuntimeConfigurationError(ADMIN_PASSWORD_MISSING)

        val passwordString = password.joinToString("")
        if (passwordString.contains(username)) throw RuntimeConfigurationError(ADMIN_PASSWORD_CANNOT_CONTAIN_PART_OF_LOGIN)

        if (password.size < ADMIN_PASSWORD_MIN_LENGTH || password.size > ADMIN_PASSWORD_MAX_LENGTH)
            throw RuntimeConfigurationError(ADMIN_PASSWORD_LENGTH_ERROR)

        var passCategoriesCount = 0
        if (adminPasswordLowerCaseRegex.containsMatchIn(passwordString)) passCategoriesCount++
        if (adminPasswordUpperCaseRegex.containsMatchIn(passwordString)) passCategoriesCount++
        if (adminPasswordDigitRegex.containsMatchIn(passwordString)) passCategoriesCount++
        if (adminPasswordNonAlphaNumericRegex.containsMatchIn(passwordString)) passCategoriesCount++

        if (passCategoriesCount < 3) throw RuntimeConfigurationError(ADMIN_PASSWORD_CATEGORY_CHECK_FAILED)
    }

    /**
     * Validate if password matches the confirmation password value
     */
    @Throws(RuntimeConfigurationError::class)
    private fun checkPasswordsMatch(password: CharArray, confirmPassword: CharArray) {
        if (!password.contentEquals(confirmPassword))
            throw RuntimeConfigurationError(ADMIN_PASSWORD_DOES_NOT_MATCH)
    }

    //endregion Admin Login/Password

    //region Collation

    /**
     * Validate SQL Database Collation setting
     */
    @Throws(RuntimeConfigurationError::class)
    private fun validateCollation(collation: String) {
        checkValueIsSet(collation, SQL_DATABASE_COLLATION_MISSING)
    }

    //endregion Collation

    /**
     * Validate Azure resource name against Azure requirements
     * Please see for details - https://docs.microsoft.com/en-us/azure/architecture/best-practices/naming-conventions
     *
     * @throws [RuntimeConfigurationError] in case name does not match requirements
     */
    @Throws(RuntimeConfigurationError::class)
    private fun validateResourceName(name: String,
                                     nameLengthMin: Int,
                                     nameLengthMax: Int,
                                     nameLengthErrorMessage: String,
                                     nameRegex: Regex,
                                     nameInvalidCharsMessage: String) {

        validateResourceNameRegex(name, nameRegex, nameInvalidCharsMessage)

        if (name.length < nameLengthMin || name.length > nameLengthMax)
            throw RuntimeConfigurationError(nameLengthErrorMessage)
    }

    @Throws(RuntimeConfigurationError::class)
    private fun validateResourceNameRegex(name: String,
                                          nameRegex: Regex,
                                          nameInvalidCharsMessage: String) {
        val matches = nameRegex.findAll(name)
        if (matches.count() > 0) {
            val invalidChars = matches.map { it.value }.distinct().joinToString("', '", "'", "'")
            throw RuntimeConfigurationError(String.format(nameInvalidCharsMessage, invalidChars))
        }
    }

    /**
     * Validate the field is set in a configuration
     *
     * @param value filed value to validate
     * @param message failure message to show to a user
     *
     * @throws [RuntimeConfigurationError] if field value is not set
     */
    @Throws(RuntimeConfigurationError::class)
    private fun checkValueIsSet(value: String, message: String) {
        if (value.isEmpty()) throw RuntimeConfigurationError(message)
    }
}
