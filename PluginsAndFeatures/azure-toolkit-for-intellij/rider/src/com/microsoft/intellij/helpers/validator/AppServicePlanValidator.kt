/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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
import org.jetbrains.plugins.azure.RiderAzureBundle.message

object AppServicePlanValidator : AzureResourceValidator() {

    private const val APP_SERVICE_PLAN_NAME_MIN_LENGTH = 1
    private const val APP_SERVICE_PLAN_NAME_MAX_LENGTH = 40

    private val appServicePlanNameRegex = "[^\\p{L}0-9-]".toRegex()

    private val nameLengthErrorMessage =
            message("run_config.publish.validation.service_plan.name_length_error", APP_SERVICE_PLAN_NAME_MIN_LENGTH, APP_SERVICE_PLAN_NAME_MAX_LENGTH)

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
            checkValueIsSet(plan, message("run_config.publish.validation.service_plan.not_defined"))

    fun checkAppServicePlanIdIsSet(planId: String?) =
            checkValueIsSet(planId, message("run_config.publish.validation.service_plan.id_not_defined"))

    fun checkAppServicePlanNameIsSet(name: String) =
            checkValueIsSet(name, message("run_config.publish.validation.service_plan.name_not_defined"))

    fun checkAppServicePlanNameMinLength(name: String) =
            checkNameMinLength(
                    name = name,
                    minLength = APP_SERVICE_PLAN_NAME_MIN_LENGTH,
                    errorMessage = nameLengthErrorMessage)

    fun checkAppServicePlanNameMaxLength(name: String) =
            checkNameMaxLength(
                    name = name,
                    maxLength = APP_SERVICE_PLAN_NAME_MAX_LENGTH,
                    errorMessage = nameLengthErrorMessage)

    fun checkInvalidCharacters(name: String) =
            validateResourceNameRegex(name, appServicePlanNameRegex, "${message("run_config.publish.validation.service_plan.name_invalid")} %s.")

    fun checkAppServicePlanNameExists(name: String): ValidationResult {
        val status = ValidationResult()
        if (isAppServicePlanExist(name)) return status.setInvalid(message("run_config.publish.validation.service_plan.already_exists", name))
        return status
    }

    private fun isAppServicePlanExist(appServicePlanName: String): Boolean {
        val resourceGroupToAppServicePlanMap = AzureModel.getInstance().resourceGroupToAppServicePlanMap ?: return false

        val appServicePlans = resourceGroupToAppServicePlanMap.flatMap { it.value }
        return appServicePlans.any { it.name().equals(appServicePlanName, true) }
    }
}
