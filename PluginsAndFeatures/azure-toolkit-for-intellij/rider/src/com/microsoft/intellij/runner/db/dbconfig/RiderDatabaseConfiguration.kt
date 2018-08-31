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
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.utils.AzureModel
import com.microsoft.intellij.runner.AzureRunConfigurationBase
import com.microsoft.intellij.runner.db.AzureDatabaseSettingModel
import java.io.IOException

class RiderDatabaseConfiguration(project: Project,
                                 factory: ConfigurationFactory,
                                 name: String)
    : AzureRunConfigurationBase<AzureDatabaseSettingModel>(project, factory, name) {

    companion object {

        private const val SIGN_IN_REQUIRED = "Please sign in with your Azure account"

        private const val SUBSCRIPTION_MISSING = "Subscription not provided"

        private const val SQL_DATABASE_MISSING = "Please select a SQL Database"
        private const val SQL_DATABASE_NAME_REGEX_STRING = "^[-_].+[-_]$"
        private const val SQL_DATABASE_NAME_MISSING = "Database name not provided"
        private const val SQL_DATABASE_NAME_INVALID = "Database name cannot contain characters: %s"

        private const val SQL_SERVER_MISSING = "Please select a SQL Server"
        private const val SQL_SERVER_NAME_REGEX_STRING = "^[-_].+[-_]$"

        private const val RESOURCE_GROUP_MISSING = "Please select a Resource Group"
        private const val RESOURCE_GROUP_ALREADY_EXISTS = "Resource Group with name '%s' already exists"
        private const val RESOURCE_GROUP_REGEX_STRING = "[^\\p{L}0-9-.()_]"
        private const val RESOURCE_GROUP_NAME_MIN_LENGTH = 1
        private const val RESOURCE_GROUP_NAME_MAX_LENGTH = 90
        private const val RESOURCE_GROUP_NAME_MISSING = "Resource Group name not provided"
        private const val RESOURCE_GROUP_NAME_LENGTH_ERROR =
                "Resource Group name should be from $RESOURCE_GROUP_NAME_MIN_LENGTH to $RESOURCE_GROUP_NAME_MAX_LENGTH characters"
        private const val RESOURCE_GROUP_NAME_INVALID = "Resource Group name cannot contain characters: %s"
        private const val RESOURCE_GROUP_NAME_CANNOT_ENDS_WITH_PERIOD = "Resource Group name cannot ends with period symbol"

        private const val LOCATION_MISSING = "Location not provided"

        private const val PRICING_TIER_MISSING = "Pricing Tier not provided"

        private const val PROJECT_MISSING = "Please select a project to deploy"
        private const val ADMIN_PASSWORD_MISSING = "Administrator password not provided"
        private const val ADMIN_PASSWORD_CONFIRM_MISSING = "Administrator password confirmation not provided"
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
     * TODO: ...
     * Validate the configuration to run
     *
     * @throws [RuntimeConfigurationError] when configuration miss expected fields
     */
    @Throws(RuntimeConfigurationError::class)
    override fun checkConfiguration() {

        validateSignIn()

        checkValueSet(myModel.subscriptionId, SUBSCRIPTION_MISSING)

        validateDatabaseName(myModel.databaseName)

        if (myModel.isCreatingResourceGroup) {
            validateResourceGroupName(myModel.resourceGroupName)
        }

        if (myModel.isCreatingSqlServer) {
            validateSqlServerName(myModel.sqlServerName)

            val adminPassword = validatePassword(myModel.sqlServerAdminPassword)
            val adminPasswordConfirm = validatePassword(myModel.sqlServerAdminPasswordConfirm)
            checkPasswordsMatch(adminPassword, adminPasswordConfirm)
        }
    }

    //region Sign In

    /**
     * Check whether user is signed in to Azure account
     *
     * @throws [RuntimeConfigurationError] in case validation is failed
     */
    @Throws(RuntimeConfigurationError::class)
    private fun validateSignIn() {
        try {
            if (!AuthMethodManager.getInstance().isSignedIn) { throw RuntimeConfigurationError(SIGN_IN_REQUIRED) }
        } catch (e: IOException) {
            throw RuntimeConfigurationError(SIGN_IN_REQUIRED)
        }
    }

    //endregion Sign In

    //region SQL Server

    // TODO: SD -- Add validation
    private fun validateSqlServerName(name: String?) {
    }

    //endregion SQL Server

    //region SQL Database

    private fun validateDatabaseName(name: String?) {
        if (name == null || name.isEmpty()) throw RuntimeConfigurationError(SQL_DATABASE_NAME_MISSING)

        val regex = SQL_DATABASE_NAME_REGEX_STRING.toRegex()
        val matches = regex.findAll(name)
        if (matches.count() > 0) {
            val invalidChars = matches.map { it.value }.distinct().joinToString("', '", "'", "'")
            throw RuntimeConfigurationError(String.format(SQL_DATABASE_NAME_INVALID, invalidChars))
        }
    }

    //endregion SQL Database

    //region Resource Group

    @Throws(RuntimeConfigurationError::class)
    private fun validateResourceGroupName(name: String?) {

        if (name == null || name.isEmpty()) throw RuntimeConfigurationError(RESOURCE_GROUP_NAME_MISSING)
        val subscriptionId = myModel.subscriptionId
        if (subscriptionId != null) checkResourceGroupExistence(subscriptionId, name)

        if (name.endsWith('.')) throw RuntimeConfigurationError(RESOURCE_GROUP_NAME_CANNOT_ENDS_WITH_PERIOD)

        validateResourceName(name,
                RESOURCE_GROUP_NAME_MIN_LENGTH,
                RESOURCE_GROUP_NAME_MAX_LENGTH,
                RESOURCE_GROUP_NAME_LENGTH_ERROR,
                RESOURCE_GROUP_REGEX_STRING.toRegex(),
                RESOURCE_GROUP_NAME_INVALID)
    }

    @Throws(RuntimeConfigurationError::class)
    private fun checkResourceGroupExistence(subscriptionId: String, resourceGroupName: String) {
        val subscriptionToResourceGroupMap = AzureModel.getInstance().subscriptionToResourceGroupMap

        if (subscriptionToResourceGroupMap != null) {
            val resourceGroups = subscriptionToResourceGroupMap.filter { it.key.subscriptionId == subscriptionId }.firstOrNull()?.value
            if (resourceGroups != null && resourceGroups.any { it.name().equals(resourceGroupName, true) })
                throw RuntimeConfigurationError(String.format(RESOURCE_GROUP_ALREADY_EXISTS, resourceGroupName))
        }
    }

    //endregion Resource Group

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

        val matches = nameRegex.findAll(name)
        if (matches.count() > 0) {
            val invalidChars = matches.map { it.value }.distinct().joinToString("', '", "'", "'")
            throw RuntimeConfigurationError(String.format(nameInvalidCharsMessage, invalidChars))
        }

        if (name.length < nameLengthMin || name.length > nameLengthMax)
            throw RuntimeConfigurationError(nameLengthErrorMessage)
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
    private fun checkValueSet(value: String?, message: String): String {
        if (value == null || value.isEmpty()) throw RuntimeConfigurationError(message)
        return value
    }

    @Throws(RuntimeConfigurationError::class)
    private fun validatePassword(password: CharArray?): CharArray {
        // TODO: There should be some set of rules that define the "good" password
        // TODO: Check azure
        if (password == null || password.isEmpty()) throw RuntimeConfigurationError(ADMIN_PASSWORD_MISSING)
        return password
    }

    @Throws(RuntimeConfigurationError::class)
    private fun validateUserLogin(username: String?, password: CharArray?) {
        // TODO: Check that we can authenticate with those credentials
    }

    @Throws(RuntimeConfigurationError::class)
    private fun checkPasswordsMatch(password: CharArray, confirmPassword: CharArray) {
        if (!password.contentEquals(confirmPassword))
            throw RuntimeConfigurationError(ADMIN_PASSWORD_DOES_NOT_MATCH)
    }
}
