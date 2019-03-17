/**
 * Copyright (c) 2019 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.functionapp.config.validator

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.microsoft.azure.management.appservice.FunctionApp
import com.microsoft.intellij.helpers.validator.*
import com.microsoft.intellij.runner.functionapp.model.FunctionAppPublishModel
import com.microsoft.intellij.runner.validator.ConfigurationValidator

object FunctionAppConfigValidator : ConfigurationValidator() {

    @Throws(RuntimeConfigurationError::class)
    fun validateFunctionApp(model: FunctionAppPublishModel) {
        checkStatus(SubscriptionValidator.validateSubscription(model.subscription))
        val subscriptionId = model.subscription!!.subscriptionId()

        if (model.isCreatingNewApp) {
            checkStatus(FunctionAppValidator.validateFunctionAppName(subscriptionId, model.appName))

            if (model.isCreatingResourceGroup) {
                checkStatus(ResourceGroupValidator.validateResourceGroupName(model.resourceGroupName))
                checkStatus(ResourceGroupValidator.checkResourceGroupExistence(subscriptionId, model.resourceGroupName))
            } else {
                checkStatus(ResourceGroupValidator.checkResourceGroupNameIsSet(model.resourceGroupName))
            }

            if (model.isCreatingAppServicePlan) {
                checkStatus(AppServicePlanValidator.validateAppServicePlanName(model.appServicePlanName))
                checkStatus(LocationValidator.checkLocationIsSet(model.location))
                checkStatus(PricingTierValidator.checkPricingTierIsSet(model.pricingTier))
            } else {
                checkStatus(AppServicePlanValidator.checkAppServicePlanIdIsSet(model.appServicePlanId))
            }

            if (model.isCreatingStorageAccount) {
                checkStatus(StorageAccountValidator.validateStorageAccountName(subscriptionId, model.storageAccountName))
                checkStatus(StorageAccountValidator.checkStorageAccountTypeIsSet(model.storageAccountType))
            } else {
                checkStatus(StorageAccountValidator.checkStorageAccountIsSet(model.storageAccount))
            }
        } else {
            checkStatus(FunctionAppValidator.checkFunctionAppIsSet(model.app))
        }
    }

    /**
     * Check Connection String name existence for a web app
     *
     * @param name connection string name
     * @param webAppId a web app to check
     * @throws [RuntimeConfigurationError] when connection string with a specified name already configured for a web app
     */
    @Throws(RuntimeConfigurationError::class)
    fun checkConnectionStringNameExistence(name: String, app: FunctionApp) =
            checkStatus(FunctionAppValidator.checkConnectionStringNameExistence(name, app))
}