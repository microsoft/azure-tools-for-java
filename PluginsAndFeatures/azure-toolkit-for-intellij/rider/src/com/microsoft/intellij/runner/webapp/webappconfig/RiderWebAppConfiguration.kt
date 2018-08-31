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
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.utils.AzureModel
import com.microsoft.intellij.runner.AzureRunConfigurationBase
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppSettingModel
import java.io.File
import java.io.IOException

class RiderWebAppConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
        AzureRunConfigurationBase<AzureDotNetWebAppSettingModel>(project, factory, name) {

    companion object {
        private const val SIGN_IN_REQUIRED = "Please sign in with your Azure account"

        private const val PROJECT_MISSING = "Please select a project to deploy"
        private const val PROJECT_TARGETS_MISSING = "Selected project '%s' cannot be published. Please choose a Web App"

        private const val SUBSCRIPTION_MISSING = "Subscription not provided"

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

        private const val WEB_APP_MISSING = "Please select an Azure Web App"
        private const val WEB_APP_ALREADY_EXISTS = "Web App with name '%s' already exists"
        private const val WEB_APP_NAME_REGEX_STRING = "[^\\p{L}0-9-]"
        private const val WEB_APP_NAME_MIN_LENGTH = 2
        private const val WEB_APP_NAME_MAX_LENGTH = 60
        private const val WEB_APP_NAME_MISSING = "Web App name not provided"
        private const val WEB_APP_NAME_LENGTH_ERROR =
                "Web App name should be from $WEB_APP_NAME_MIN_LENGTH to $WEB_APP_NAME_MAX_LENGTH characters"
        private const val WEB_APP_NAME_INVALID = "Web App name cannot contain characters: %s"
        private const val WEB_APP_NAME_CANNOT_START_END_WITH_DASH = "Web App name cannot begin or end with dash symbol"

        private const val APP_SERVICE_PLAN_MISSING = "Please select an App Service Plan"
        private const val APP_SERVICE_PLAN_ALREADY_EXISTS = "App Service Plan with name '%s' already exists"
        private const val APP_SERVICE_PLAN_NAME_REGEX_STRING = "[^\\p{L}0-9-]"
        private const val APP_SERVICE_PLAN_NAME_MIN_LENGTH = 1
        private const val APP_SERVICE_PLAN_NAME_MAX_LENGTH = 40
        private const val APP_SERVICE_PLAN_NAME_MISSING = "App Service Plan name not provided"
        private const val APP_SERVICE_PLAN_NAME_LENGTH_ERROR =
                "Web App name should be from $WEB_APP_NAME_MIN_LENGTH to $WEB_APP_NAME_MAX_LENGTH characters"
        private const val APP_SERVICE_PLAN_NAME_INVALID = "App Service Plan name cannot contain characters: %s"
        private const val LOCATION_MISSING = "Location not provided"
        private const val PRICING_TIER_MISSING = "Pricing Tier not provided"

        private const val WEB_APP_TARGET_NAME = "Microsoft.WebApplication.targets"
    }

    private val myModel: AzureDotNetWebAppSettingModel = AzureDotNetWebAppSettingModel()

    override fun getSubscriptionId(): String? {
        return myModel.subscriptionId
    }

    override fun getTargetPath(): String? {
        return myModel.publishableProject?.projectFilePath
    }

    override fun getTargetName(): String? {
        return myModel.publishableProject?.projectName
    }

    init {
        myModel.publishableProject = project.solution.publishableProjectsModel.publishableProjects.values
                .sortedWith(compareBy({ it.isWeb }, { it.projectName })).firstOrNull()
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
        validateSignIn()
        validateProject()

        if (myModel.isCreatingWebApp) {
            validateWebAppName(myModel.webAppName)
            checkValueSet(myModel.subscriptionId, SUBSCRIPTION_MISSING)

            if (myModel.isCreatingResourceGroup) {
                validateResourceGroupName(myModel.resourceGroupName)
            } else {
                checkValueSet(myModel.resourceGroupName, RESOURCE_GROUP_MISSING)
            }

            if (myModel.isCreatingAppServicePlan) {
                checkValueSet(myModel.region, LOCATION_MISSING)
                checkValueSet(myModel.pricingTier, PRICING_TIER_MISSING)
                validateAppServicePlanName(myModel.appServicePlanName)
            } else {
                checkValueSet(myModel.appServicePlanId, APP_SERVICE_PLAN_MISSING)
            }
        } else {
            checkValueSet(myModel.webAppId, WEB_APP_MISSING)
        }
    }

    //region Sign In

    /**
     * Check whether user is signed in to Azure account
     *
     * @throws [ConfigurationException] in case validation is failed
     */
    @Throws(RuntimeConfigurationError::class)
    private fun validateSignIn() {
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
     * Validate publishable project selection in the config
     * Check for the WebApplication targets that contains tasks for generating publishable package on Windows only
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
     * @return [Boolean] whether WebApplication targets are present in publishable project
     */
    private fun isWebTargetsPresent(csprojFile: File): Boolean = csprojFile.readText().contains(WEB_APP_TARGET_NAME, true)

    //endregion Project

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

    // TODO: There is a method in [azure.resourceGroups().checkExistence()]
    // TODO: It might be better to call this method async
    // TODO: We cannot do it easily, because we cannot throw from the async result
    @Throws(RuntimeConfigurationError::class)
    private fun checkResourceGroupExistence(subscriptionId: String, resourceGroupName: String) {
        val subscriptionToResourceGroupMap = AzureModel.getInstance().subscriptionToResourceGroupMap

        if (subscriptionToResourceGroupMap != null) {
            val resourceGroups = subscriptionToResourceGroupMap.filter { it.key.subscriptionId == subscriptionId }.firstOrNull()?.value
            if (resourceGroups != null && resourceGroups.any { it.name().equals(resourceGroupName, true) })
                throw RuntimeConfigurationError(String.format(RESOURCE_GROUP_ALREADY_EXISTS, resourceGroupName))
        }

//        AzureDotNetWebAppMvpModel.checkResourceGroupExistenceAsync(subscriptionId, resourceGroupName)
//                .subscribe { if (it) throw RuntimeConfigurationError(String.format(RESOURCE_GROUP_ALREADY_EXISTS, resourceGroupName)) }

    }

    //endregion Resource Group

    //region Web App

    // Please see for details -
    // https://docs.microsoft.com/en-us/azure/app-service/app-service-web-get-started-dotnet?toc=%2Fen-us%2Fdotnet%2Fapi%2Fazure_ref_toc%2Ftoc.json&bc=%2Fen-us%2Fdotnet%2Fazure_breadcrumb%2Ftoc.json&view=azure-dotnet#create-an-app-service-plan
    @Throws(RuntimeConfigurationError::class)
    private fun validateWebAppName(name: String?) {

        val webAppName = checkValueSet(name, WEB_APP_NAME_MISSING)
        checkWebAppExistence(webAppName)

        if (webAppName.startsWith('-') || webAppName.endsWith('-')) throw RuntimeConfigurationError(WEB_APP_NAME_CANNOT_START_END_WITH_DASH)

        validateResourceName(webAppName,
                WEB_APP_NAME_MIN_LENGTH,
                WEB_APP_NAME_MAX_LENGTH,
                WEB_APP_NAME_LENGTH_ERROR,
                WEB_APP_NAME_REGEX_STRING.toRegex(),
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
    private fun validateAppServicePlanName(name: String?) {

        val planName = checkValueSet(name, APP_SERVICE_PLAN_NAME_MISSING)
        checkAppServicePlanExistence(planName)

        validateResourceName(planName,
                APP_SERVICE_PLAN_NAME_MIN_LENGTH,
                APP_SERVICE_PLAN_NAME_MAX_LENGTH,
                APP_SERVICE_PLAN_NAME_LENGTH_ERROR,
                APP_SERVICE_PLAN_NAME_REGEX_STRING.toRegex(),
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

    /**
     * Validate Azure resource name against Azure requirements
     * Please see for details - https://docs.microsoft.com/en-us/azure/architecture/best-practices/naming-conventions
     *
     * @throws [ConfigurationException] in case name does not match requirements
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
    private fun checkValueSet(value: PricingTier?, message: String): PricingTier {
        if (value == null) throw RuntimeConfigurationError(message)
        return value
    }
}
