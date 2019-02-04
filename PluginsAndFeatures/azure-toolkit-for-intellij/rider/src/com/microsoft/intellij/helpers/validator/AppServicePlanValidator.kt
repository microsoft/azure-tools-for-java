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

import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azuretools.utils.AzureModel

object AppServicePlanValidator : AzureResourceValidator() {

    private const val APP_SERVICE_PLAN_NOT_DEFINED = "App Service Plan not provided."
    private const val APP_SERVICE_PLAN_NAME_NOT_DEFINED = "App Service Plan name not provided."
    private const val APP_SERVICE_PLAN_ID_NOT_DEFINED = "App Service Plan ID is not defined."
    private const val APP_SERVICE_PLAN_NAME_INVALID = "App Service Plan name cannot contain characters: %s."
    private const val APP_SERVICE_PLAN_ALREADY_EXISTS = "App Service Plan with name '%s' already exists."

    private val appServicePlanNameRegex = "[^\\p{L}0-9-]".toRegex()
    private const val APP_SERVICE_PLAN_NAME_MIN_LENGTH = 1
    private const val APP_SERVICE_PLAN_NAME_MAX_LENGTH = 40
    private const val APP_SERVICE_PLAN_NAME_LENGTH_ERROR =
            "Web App name should be from $APP_SERVICE_PLAN_NAME_MIN_LENGTH to $APP_SERVICE_PLAN_NAME_MAX_LENGTH characters."

    fun validateAppServicePlanName(name: String): ValidationResult {

        val status = checkAppServicePlanNameIsSet(name)
        if (!status.isValid) return status

        status.merge(checkAppServicePlanNameExists(name))
        if (!status.isValid) return status

        return status
                .merge(checkAppServicePlanNameMinLength(name))
                .merge(checkAppServicePlanNameMaxLength(name))
                .merge(checkInvalidCharacters(name))
    }

    fun checkAppServicePlanIsSet(plan: AppServicePlan?) =
            checkValueIsSet(plan, APP_SERVICE_PLAN_NOT_DEFINED)

    fun checkAppServicePlanIdIsSet(planId: String?) =
            checkValueIsSet(planId, APP_SERVICE_PLAN_ID_NOT_DEFINED)

    fun checkAppServicePlanNameIsSet(name: String) =
            checkValueIsSet(name, APP_SERVICE_PLAN_NAME_NOT_DEFINED)

    fun checkAppServicePlanNameMinLength(name: String) =
            checkNameMinLength(name, APP_SERVICE_PLAN_NAME_MIN_LENGTH, APP_SERVICE_PLAN_NAME_LENGTH_ERROR)

    fun checkAppServicePlanNameMaxLength(name: String) =
            checkNameMaxLength(name, APP_SERVICE_PLAN_NAME_MAX_LENGTH, APP_SERVICE_PLAN_NAME_LENGTH_ERROR)

    fun checkInvalidCharacters(name: String) =
            validateResourceNameRegex(name, appServicePlanNameRegex, APP_SERVICE_PLAN_NAME_INVALID)

    fun checkAppServicePlanNameExists(name: String): ValidationResult {
        val status = ValidationResult()
        if (isAppServicePlanExist(name)) return status.setInvalid(String.format(APP_SERVICE_PLAN_ALREADY_EXISTS, name))
        return status
    }

    private fun isAppServicePlanExist(appServicePlanName: String): Boolean {
        val resourceGroupToAppServicePlanMap = AzureModel.getInstance().resourceGroupToAppServicePlanMap ?: return false

        val appServicePlans = resourceGroupToAppServicePlanMap.flatMap { it.value }
        return appServicePlans.any { it.name().equals(appServicePlanName, true) }
    }
}
