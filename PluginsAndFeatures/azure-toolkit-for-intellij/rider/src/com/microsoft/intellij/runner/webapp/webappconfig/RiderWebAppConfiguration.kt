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


        private val resourceGroupRegex = "[^\\p{L}0-9-.()_]".toRegex()
        private const val RESOURCE_GROUP_NAME_MIN_LENGTH = 1
        private const val RESOURCE_GROUP_NAME_MAX_LENGTH = 90
        private const val RESOURCE_GROUP_NAME_LENGTH_ERROR =
                "Resource Group name should be from $RESOURCE_GROUP_NAME_MIN_LENGTH to $RESOURCE_GROUP_NAME_MAX_LENGTH characters"


        private val webAppNameRegex = "[^\\p{L}0-9-]".toRegex()
        private const val WEB_APP_NAME_MIN_LENGTH = 2
        private const val WEB_APP_NAME_MAX_LENGTH = 60
        private const val WEB_APP_NAME_LENGTH_ERROR =
                "Web App name should be from $WEB_APP_NAME_MIN_LENGTH to $WEB_APP_NAME_MAX_LENGTH characters"

        private val appServicePlanNameRegex = "[^\\p{L}0-9-]".toRegex()
        private const val APP_SERVICE_PLAN_NAME_MIN_LENGTH = 1
        private const val APP_SERVICE_PLAN_NAME_MAX_LENGTH = 40
        private const val APP_SERVICE_PLAN_NAME_LENGTH_ERROR =
                "Web App name should be from $WEB_APP_NAME_MIN_LENGTH to $WEB_APP_NAME_MAX_LENGTH characters"


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

    }

    private val myModel = AzureDotNetWebAppSettingModel()

    init {
        myModel.webAppModel.publishableProject = project.solution.publishableProjectsModel.publishableProjects.values
                .sortedWith(compareBy({ it.isWeb }, { it.projectName })).firstOrNull()
    }

    override fun getSubscriptionId(): String {
        return myModel.webAppModel.subscriptionId
    }

    override fun getTargetPath(): String {
        return myModel.webAppModel.publishableProject?.projectFilePath ?: ""
    }

    override fun getTargetName(): String {
        return myModel.webAppModel.publishableProject?.projectName ?: ""
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

        validateWebApp(myModel.webAppModel)
        validateDatabaseConnection(myModel.databaseModel)

        if (myModel.databaseModel.isDatabaseConnectionEnabled) {
            checkConnectionStringNameExistence(myModel.databaseModel.connectionStringName, myModel.webAppModel.webAppId)
        }
    }

    @Throws(RuntimeConfigurationError::class)
    private fun validateWebApp(model: AzureDotNetWebAppSettingModel.WebAppModel) {
        validateProject()

        if (model.isCreatingWebApp) {
            validateWebAppName(model.webAppName)
            checkValueIsSet(model.subscriptionId, UiConstants.SUBSCRIPTION_NOT_DEFINED)

            if (model.isCreatingResourceGroup) {
                validateResourceGroupName(model.subscriptionId, model.resourceGroupName)
            } else {
                checkValueIsSet(model.resourceGroupName, UiConstants.RESOURCE_GROUP_NAME_NOT_DEFINED)
            }

            if (model.isCreatingAppServicePlan) {
                validateAppServicePlanName(model.appServicePlanName)
                checkValueIsSet(model.location, UiConstants.LOCATION_NOT_DEFINED)
            } else {
                checkValueIsSet(model.appServicePlanId, UiConstants.APP_SERVICE_PLAN_ID_NOT_DEFINED)
            }

        } else {
            checkValueIsSet(model.webAppId, UiConstants.WEB_APP_ID_NOT_DEFINED)
        }
    }

    @Throws(RuntimeConfigurationError::class)
    private fun validateDatabaseConnection(model: AzureDotNetWebAppSettingModel.DatabaseModel) {
        if (!model.isDatabaseConnectionEnabled) return

        if (model.isCreatingSqlDatabase) {
            validateDatabaseName(model.subscriptionId, model.databaseName, model.sqlServerName)

            if (model.isCreatingDbResourceGroup) {
                validateResourceGroupName(model.subscriptionId, model.dbResourceGroupName)
            } else {
                checkValueIsSet(model.dbResourceGroupName, UiConstants.RESOURCE_GROUP_NAME_NOT_DEFINED)
            }

            if (model.isCreatingSqlServer) {
                validateSqlServerName(model.subscriptionId, model.sqlServerName)
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
                throw RuntimeConfigurationError(UiConstants.SIGN_IN_REQUIRED)
            }
        } catch (e: IOException) {
            throw RuntimeConfigurationError(UiConstants.SIGN_IN_REQUIRED)
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

        val project = myModel.webAppModel.publishableProject ?: throw RuntimeConfigurationError(UiConstants.PROJECT_NOT_DEFINED)

        if (!project.isDotNetCore && !isWebTargetsPresent(File(project.projectFilePath)))
            throw RuntimeConfigurationError(String.format(UiConstants.PROJECT_TARGETS_NOT_DEFINED, UiConstants.WEB_APP_TARGET_NAME))
    }

    /**
     * Check whether necessary targets exists in a project that are necessary for web app deployment
     * Note: On Windows only
     *
     * TODO: We should replace this method with a target validation on a backend (RIDER-18500)
     *
     * @return [Boolean] whether WebApplication targets are present in publishable project
     */
    private fun isWebTargetsPresent(csprojFile: File): Boolean = csprojFile.readText().contains(UiConstants.WEB_APP_TARGET_NAME, true)

    //endregion Project

    //region Resource Group

    @Throws(RuntimeConfigurationError::class)
    private fun validateResourceGroupName(subscriptionId: String, name: String) {

        checkValueIsSet(name, UiConstants.RESOURCE_GROUP_NAME_NOT_DEFINED)
        checkResourceGroupExistence(subscriptionId, name)

        if (name.endsWith('.')) throw RuntimeConfigurationError(UiConstants.RESOURCE_GROUP_NAME_CANNOT_ENDS_WITH_PERIOD)

        validateResourceName(name,
                RESOURCE_GROUP_NAME_MIN_LENGTH,
                RESOURCE_GROUP_NAME_MAX_LENGTH,
                RESOURCE_GROUP_NAME_LENGTH_ERROR,
                resourceGroupRegex,
                UiConstants.RESOURCE_GROUP_NAME_INVALID)
    }

    @Throws(RuntimeConfigurationError::class)
    private fun checkResourceGroupExistence(subscriptionId: String, resourceGroupName: String) {
        val subscriptionToResourceGroupMap = AzureModel.getInstance().subscriptionToResourceGroupMap

        if (subscriptionToResourceGroupMap != null) {
            val resourceGroups = subscriptionToResourceGroupMap.filter { it.key.subscriptionId == subscriptionId }.firstOrNull()?.value
            if (resourceGroups != null && resourceGroups.any { it.name().equals(resourceGroupName, true) })
                throw RuntimeConfigurationError(String.format(UiConstants.RESOURCE_GROUP_ALREADY_EXISTS, resourceGroupName))
        }
    }

    //endregion Resource Group

    //region Web App

    // Please see for details -
    // https://docs.microsoft.com/en-us/azure/app-service/app-service-web-get-started-dotnet?toc=%2Fen-us%2Fdotnet%2Fapi%2Fazure_ref_toc%2Ftoc.json&bc=%2Fen-us%2Fdotnet%2Fazure_breadcrumb%2Ftoc.json&view=azure-dotnet#create-an-app-service-plan
    @Throws(RuntimeConfigurationError::class)
    private fun validateWebAppName(name: String) {

        checkValueIsSet(name, UiConstants.WEB_APP_NAME_NOT_DEFINED)
        checkWebAppExistence(name)

        if (name.startsWith('-') || name.endsWith('-')) throw RuntimeConfigurationError(UiConstants.WEB_APP_NAME_CANNOT_START_END_WITH_DASH)

        validateResourceName(name,
                WEB_APP_NAME_MIN_LENGTH,
                WEB_APP_NAME_MAX_LENGTH,
                WEB_APP_NAME_LENGTH_ERROR,
                webAppNameRegex,
                UiConstants.WEB_APP_NAME_INVALID)
    }

    @Throws(RuntimeConfigurationError::class)
    private fun checkWebAppExistence(webAppName: String) {
        val resourceGroupToWebAppMap = AzureModel.getInstance().resourceGroupToWebAppMap

        if (resourceGroupToWebAppMap != null) {
            val webApps = resourceGroupToWebAppMap.flatMap { it.value }
            if (webApps.any { it.name().equals(webAppName, true) })
                throw RuntimeConfigurationError(String.format(UiConstants.WEB_APP_ALREADY_EXISTS, webAppName))
        }
    }

    //endregion Web App

    //region App Service Plan

    @Throws(RuntimeConfigurationError::class)
    private fun validateAppServicePlanName(name: String) {

        checkValueIsSet(name, UiConstants.APP_SERVICE_PLAN_NAME_NOT_DEFINED)
        checkAppServicePlanExistence(name)

        validateResourceName(name,
                APP_SERVICE_PLAN_NAME_MIN_LENGTH,
                APP_SERVICE_PLAN_NAME_MAX_LENGTH,
                APP_SERVICE_PLAN_NAME_LENGTH_ERROR,
                appServicePlanNameRegex,
                UiConstants.APP_SERVICE_PLAN_NAME_INVALID)
    }

    @Throws(RuntimeConfigurationError::class)
    private fun checkAppServicePlanExistence(appServicePlanName: String) {
        val resourceGroupToAppServicePlanMap = AzureModel.getInstance().resourceGroupToAppServicePlanMap

        if (resourceGroupToAppServicePlanMap != null) {
            val appServicePlans = resourceGroupToAppServicePlanMap.flatMap { it.value }
            if (appServicePlans.any { it.name().equals(appServicePlanName, true) })
                throw RuntimeConfigurationError(String.format(UiConstants.APP_SERVICE_PLAN_ALREADY_EXISTS, appServicePlanName))
        }
    }

    //endregion App Service Plan

    //region SQL Database

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
    private fun checkConnectionStringNameExistence(name: String, webAppId: String) {
        checkValueIsSet(name, UiConstants.CONNECTION_STRING_NAME_NOT_DEFINED)
        val webApp = AzureModel.getInstance().resourceGroupToWebAppMap
                .flatMap { it.value }
                .firstOrNull { it.id() == webAppId } ?: return

        if (AzureDotNetWebAppMvpModel.checkConnectionStringNameExists(webApp, name))
            throw RuntimeConfigurationError(String.format(UiConstants.CONNECTION_STRING_NAME_ALREADY_EXISTS, name))
    }

    //endregion SQL Database

    //region SQL Server

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

    //endregion SQL Server

    //region Admin Login/Password

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

    //endregion Admin Login/Password

    //region Database Collation

    /**
     * Validate SQL Database Collation setting
     */
    @Throws(RuntimeConfigurationError::class)
    private fun validateCollation(collation: String) {
        checkValueIsSet(collation, UiConstants.SQL_DATABASE_COLLATION_NOT_DEFINED)
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
