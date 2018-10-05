package com.microsoft.intellij.runner.webapp.webappconfig.validator

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.microsoft.azuretools.utils.AzureModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppSettingModel
import com.microsoft.intellij.runner.webapp.webappconfig.UiConstants
import com.microsoft.intellij.runner.webapp.webappconfig.validator.ProjectValidator.validateProject

object WebAppValidator : ConfigurationValidator() {

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

    @Throws(RuntimeConfigurationError::class)
    fun validateWebApp(model: AzureDotNetWebAppSettingModel.WebAppModel) {
        val subscriptionId = SubscriptionValidator.validateSubscription(model.subscription).subscriptionId()
        validateProject(model.publishableProject)

        if (model.isCreatingWebApp) {
            validateWebAppName(model.webAppName)
            checkValueIsSet(subscriptionId, UiConstants.SUBSCRIPTION_NOT_DEFINED)

            if (model.isCreatingResourceGroup) {
                ResourceGroupValidator.validateResourceGroupName(subscriptionId, model.resourceGroupName)
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
            checkValueIsSet(model.webAppId, UiConstants.WEB_APP_NOT_DEFINED)
        }
    }

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
}