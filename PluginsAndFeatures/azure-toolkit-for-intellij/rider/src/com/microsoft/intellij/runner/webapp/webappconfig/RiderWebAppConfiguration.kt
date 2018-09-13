package com.microsoft.intellij.runner.webapp.webappconfig

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.util.firstOrNull
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.utils.AzureModel
import com.microsoft.intellij.runner.AzureRunConfigurationBase
import com.microsoft.intellij.runner.db.AzureDatabaseMvpModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppSettingModel
import java.io.File
import java.io.IOException

class RiderWebAppConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
        AzureRunConfigurationBase<AzureDotNetWebAppSettingModel>(project, factory, name) {

    companion object {
        private const val SIGN_IN_REQUIRED = "Please sign in with your Azure account"

        private const val WEB_APP_TARGET_NAME = "Microsoft.WebApplication.targets"

        // Web App
        private const val PROJECT_MISSING = "Please select a project to deploy"
        private const val PROJECT_TARGETS_MISSING = "Selected project '%s' cannot be published. Please choose a Web App"

        private const val SUBSCRIPTION_MISSING = "Subscription not provided"

        private val resourceGroupRegex = "[^\\p{L}0-9-.()_]".toRegex()
        private const val RESOURCE_GROUP_MISSING = "Please select a Resource Group"
        private const val RESOURCE_GROUP_ALREADY_EXISTS = "Resource Group with name '%s' already exists"
        private const val RESOURCE_GROUP_NAME_MIN_LENGTH = 1
        private const val RESOURCE_GROUP_NAME_MAX_LENGTH = 90
        private const val RESOURCE_GROUP_NAME_MISSING = "Resource Group name not provided"
        private const val RESOURCE_GROUP_NAME_LENGTH_ERROR =
                "Resource Group name should be from $RESOURCE_GROUP_NAME_MIN_LENGTH to $RESOURCE_GROUP_NAME_MAX_LENGTH characters"
        private const val RESOURCE_GROUP_NAME_INVALID = "Resource Group name cannot contain characters: %s"
        private const val RESOURCE_GROUP_NAME_CANNOT_ENDS_WITH_PERIOD = "Resource Group name cannot ends with period symbol"

        private val webAppNameRegex = "[^\\p{L}0-9-]".toRegex()
        private const val WEB_APP_MISSING = "Please select an Azure Web App"
        private const val WEB_APP_ALREADY_EXISTS = "Web App with name '%s' already exists"
        private const val WEB_APP_NAME_MIN_LENGTH = 2
        private const val WEB_APP_NAME_MAX_LENGTH = 60
        private const val WEB_APP_NAME_MISSING = "Web App name not provided"
        private const val WEB_APP_NAME_LENGTH_ERROR =
                "Web App name should be from $WEB_APP_NAME_MIN_LENGTH to $WEB_APP_NAME_MAX_LENGTH characters"
        private const val WEB_APP_NAME_INVALID = "Web App name cannot contain characters: %s"
        private const val WEB_APP_NAME_CANNOT_START_END_WITH_DASH = "Web App name cannot begin or end with '-' symbol"

        private val appServicePlanNameRegex = "[^\\p{L}0-9-]".toRegex()
        private const val APP_SERVICE_PLAN_MISSING = "Please select an App Service Plan"
        private const val APP_SERVICE_PLAN_ALREADY_EXISTS = "App Service Plan with name '%s' already exists"
        private const val APP_SERVICE_PLAN_NAME_MIN_LENGTH = 1
        private const val APP_SERVICE_PLAN_NAME_MAX_LENGTH = 40
        private const val APP_SERVICE_PLAN_NAME_MISSING = "App Service Plan name not provided"
        private const val APP_SERVICE_PLAN_NAME_LENGTH_ERROR =
                "Web App name should be from $WEB_APP_NAME_MIN_LENGTH to $WEB_APP_NAME_MAX_LENGTH characters"
        private const val APP_SERVICE_PLAN_NAME_INVALID = "App Service Plan name cannot contain characters: %s"

        private const val LOCATION_MISSING = "Location not provided"

        // SQL Database
        private const val CONNECTION_STRING_NAME_MISSING = "Connection string name not provided"
        private const val CONNECTION_STRING_NAME_ALREADY_EXISTS = "Connection String with name '%s' already exists"

        private const val SQL_DATABASE_COLLATION_MISSING = "SQL Database Collation not provided"

        private val sqlDatabaseNameRegex = "[\\s]".toRegex()
        private const val SQL_DATABASE_MISSING = "SQL Database not provided"
        private const val SQL_DATABASE_NAME_MISSING = "SQL Database name not provided"
        private const val SQL_DATABASE_NAME_ALREADY_EXISTS = "SQL Database name '%s' already exists"
        private const val SQL_DATABASE_NAME_INVALID = "SQL Database name cannot contain characters: %s"

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

        private const val SQL_SERVER_ADMIN_LOGIN_MISSING = "SQL Database admin login name not provided"
        private const val SQL_SERVER_ADMIN_PASSWORD_MISSING = "SQL Database admin password not provided"

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
        // Note: this is not an inverse regex like others and must be validated accordingly
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

    private val myModel = AzureDotNetWebAppSettingModel()

    init {
        myModel.publishableProject = project.solution.publishableProjectsModel.publishableProjects.values
                .sortedWith(compareBy({ it.isWeb }, { it.projectName })).firstOrNull()
    }

    override fun getSubscriptionId(): String {
        return myModel.subscriptionId
    }

    override fun getTargetPath(): String {
        return myModel.publishableProject?.projectFilePath ?: ""
    }

    override fun getTargetName(): String {
        return myModel.publishableProject?.projectName ?: ""
    }

    override fun getModel(): AzureDotNetWebAppSettingModel {
        return myModel
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return RiderWebAppSettingEditor(project, this)
    }

    @Throws(ExecutionException::class)
    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState? {
        return RiderWebAppRunState(project, myModel)
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
        validateWebApp(myModel)
        validateDatabaseConnection(myModel)
    }

    @Throws(RuntimeConfigurationError::class)
    private fun validateWebApp(model: AzureDotNetWebAppSettingModel) {
        validateProject()

        if (model.isCreatingWebApp) {
            validateWebAppName(model.webAppName)
            checkValueIsSet(model.subscriptionId, SUBSCRIPTION_MISSING)

            if (model.isCreatingResourceGroup) {
                validateResourceGroupName(model.subscriptionId, model.resourceGroupName)
            } else {
                checkValueIsSet(model.resourceGroupName, RESOURCE_GROUP_MISSING)
            }

            if (model.isCreatingAppServicePlan) {
                validateAppServicePlanName(model.appServicePlanName)
                checkValueIsSet(model.location, LOCATION_MISSING)
            } else {
                checkValueIsSet(model.appServicePlanId, APP_SERVICE_PLAN_MISSING)
            }

        } else {
            checkValueIsSet(model.webAppId, WEB_APP_MISSING)

            if (model.isDatabaseConnectionEnabled)
                checkConnectionStringNameExistence(model.connectionStringName, model.webAppId)
        }
    }

    @Throws(RuntimeConfigurationError::class)
    private fun validateDatabaseConnection(model: AzureDotNetWebAppSettingModel) {
        if (!model.isDatabaseConnectionEnabled) return

        if (model.isCreatingSqlDatabase) {
            validateDatabaseName(myModel.subscriptionId, myModel.databaseName, myModel.sqlServerName)

            if (myModel.isCreatingDbResourceGroup) {
                validateResourceGroupName(myModel.subscriptionId, myModel.dbResourceGroupName)
            } else {
                checkValueIsSet(myModel.dbResourceGroupName, RESOURCE_GROUP_MISSING)
            }

            if (myModel.isCreatingSqlServer) {
                validateSqlServerName(myModel.subscriptionId, myModel.sqlServerName)
                validateAdminLogin(myModel.sqlServerAdminLogin)
                validateAdminPassword(myModel.sqlServerAdminLogin, myModel.sqlServerAdminPassword)
                checkPasswordsMatch(myModel.sqlServerAdminPassword, myModel.sqlServerAdminPasswordConfirm)
            } else {
                validateAdminPassword(myModel.sqlServerAdminLogin, myModel.sqlServerAdminPassword)
                // TODO: understand how to check that the password is valid
                checkValueIsSet(myModel.sqlServerId, SQL_SERVER_MISSING)
            }

            validateCollation(myModel.collation)
        } else {
            validateDatabase(model.database)

            checkValueIsSet(model.sqlServerAdminLogin, SQL_SERVER_ADMIN_LOGIN_MISSING)
            checkValueIsSet(model.sqlServerAdminPassword, SQL_SERVER_ADMIN_PASSWORD_MISSING)
            // TODO: understand how to check that the password is valid
        }
    }

    //region Sign In

    /**
     * Check whether user is signed in to Azure account
     *
     * @throws [ConfigurationException] in case validation is failed
     */
    @Throws(RuntimeConfigurationError::class)
    private fun validateAzureAccountIsSignedIn() {
        try {
            if (!AuthMethodManager.getInstance().isSignedIn) {
                throw RuntimeConfigurationError(SIGN_IN_REQUIRED)
            }
        } catch (e: IOException) {
            throw RuntimeConfigurationError(SIGN_IN_REQUIRED)
        }
    }

    //endregion Sign In

    //region Project

    /**
     * Validate publishable project in the config
     *
     * Note: for .NET web apps we ned to check for the "WebApplication" targets
     *       that contains tasks for generating publishable package
     *
     * @throws [ConfigurationException] in case validation is failed
     */
    @Throws(RuntimeConfigurationError::class)
    private fun validateProject() {

        val project = myModel.publishableProject ?: throw RuntimeConfigurationError(PROJECT_MISSING)

        if (!project.isDotNetCore && !isWebTargetsPresent(File(project.projectFilePath)))
            throw RuntimeConfigurationError(String.format(PROJECT_TARGETS_MISSING, WEB_APP_TARGET_NAME))
    }

    /**
     * Check whether necessary targets exists in a project that are necessary for web app deployment
     * Note: On Windows only
     *
     * TODO: We should replace this method with a target validation on a backend (RIDER-18500)
     *
     * @return [Boolean] whether WebApplication targets are present in publishable project
     */
    private fun isWebTargetsPresent(csprojFile: File): Boolean = csprojFile.readText().contains(WEB_APP_TARGET_NAME, true)

    //endregion Project

    //region Resource Group

    @Throws(RuntimeConfigurationError::class)
    private fun validateResourceGroupName(subscriptionId: String, name: String) {

        checkValueIsSet(name, RESOURCE_GROUP_NAME_MISSING)
        checkResourceGroupExistence(subscriptionId, name)

        if (name.endsWith('.')) throw RuntimeConfigurationError(RESOURCE_GROUP_NAME_CANNOT_ENDS_WITH_PERIOD)

        validateResourceName(name,
                RESOURCE_GROUP_NAME_MIN_LENGTH,
                RESOURCE_GROUP_NAME_MAX_LENGTH,
                RESOURCE_GROUP_NAME_LENGTH_ERROR,
                resourceGroupRegex,
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

    //region Web App

    // Please see for details -
    // https://docs.microsoft.com/en-us/azure/app-service/app-service-web-get-started-dotnet?toc=%2Fen-us%2Fdotnet%2Fapi%2Fazure_ref_toc%2Ftoc.json&bc=%2Fen-us%2Fdotnet%2Fazure_breadcrumb%2Ftoc.json&view=azure-dotnet#create-an-app-service-plan
    @Throws(RuntimeConfigurationError::class)
    private fun validateWebAppName(name: String) {

        checkValueIsSet(name, WEB_APP_NAME_MISSING)
        checkWebAppExistence(name)

        if (name.startsWith('-') || name.endsWith('-')) throw RuntimeConfigurationError(WEB_APP_NAME_CANNOT_START_END_WITH_DASH)

        validateResourceName(name,
                WEB_APP_NAME_MIN_LENGTH,
                WEB_APP_NAME_MAX_LENGTH,
                WEB_APP_NAME_LENGTH_ERROR,
                webAppNameRegex,
                WEB_APP_NAME_INVALID)
    }

    @Throws(RuntimeConfigurationError::class)
    private fun checkWebAppExistence(webAppName: String) {
        val resourceGroupToWebAppMap = AzureModel.getInstance().resourceGroupToWebAppMap

        if (resourceGroupToWebAppMap != null) {
            val webApps = resourceGroupToWebAppMap.flatMap { it.value }
            if (webApps.any { it.name().equals(webAppName, true) })
                throw RuntimeConfigurationError(String.format(WEB_APP_ALREADY_EXISTS, webAppName))
        }
    }

    //endregion Web App

    //region App Service Plan

    @Throws(RuntimeConfigurationError::class)
    private fun validateAppServicePlanName(name: String) {

        checkValueIsSet(name, APP_SERVICE_PLAN_NAME_MISSING)
        checkAppServicePlanExistence(name)

        validateResourceName(name,
                APP_SERVICE_PLAN_NAME_MIN_LENGTH,
                APP_SERVICE_PLAN_NAME_MAX_LENGTH,
                APP_SERVICE_PLAN_NAME_LENGTH_ERROR,
                appServicePlanNameRegex,
                APP_SERVICE_PLAN_NAME_INVALID)
    }

    @Throws(RuntimeConfigurationError::class)
    private fun checkAppServicePlanExistence(appServicePlanName: String) {
        val resourceGroupToAppServicePlanMap = AzureModel.getInstance().resourceGroupToAppServicePlanMap

        if (resourceGroupToAppServicePlanMap != null) {
            val appServicePlans = resourceGroupToAppServicePlanMap.flatMap { it.value }
            if (appServicePlans.any { it.name().equals(appServicePlanName, true) })
                throw RuntimeConfigurationError(String.format(APP_SERVICE_PLAN_ALREADY_EXISTS, appServicePlanName))
        }
    }

    //endregion App Service Plan

    //region SQL Database

    @Throws(RuntimeConfigurationError::class)
    private fun validateDatabase(database: SqlDatabase?) {
        if (database == null) throw RuntimeConfigurationError(SQL_DATABASE_MISSING)
    }

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

    @Throws(RuntimeConfigurationError::class)
    private fun checkConnectionStringNameExistence(name: String, webAppId: String) {
        checkValueIsSet(name, CONNECTION_STRING_NAME_MISSING)
        val webApp = AzureModel.getInstance().resourceGroupToWebAppMap
                .flatMap { it.value }
                .firstOrNull { it.id() == webAppId } ?: return

        if (AzureDotNetWebAppMvpModel.checkConnectionStringNameExists(webApp, name))
            throw RuntimeConfigurationError(String.format(CONNECTION_STRING_NAME_ALREADY_EXISTS, name))
    }

    //endregion SQL Database

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

    //region Database Collation

    /**
     * Validate SQL Database Collation setting
     */
    @Throws(RuntimeConfigurationError::class)
    private fun validateCollation(collation: String) {
        checkValueIsSet(collation, SQL_DATABASE_COLLATION_MISSING)
    }

    //endregion Database Collation

    //region Private Methods and Operators

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

    @Throws(RuntimeConfigurationError::class)
    private fun checkValueIsSet(value: CharArray, message: String) {
        if (value.isEmpty()) throw RuntimeConfigurationError(message)
    }

    //endregion Private Methods and Operators
}
