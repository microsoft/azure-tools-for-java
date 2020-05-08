/**
 * Copyright (c) 2020 JetBrains s.r.o.
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
import com.microsoft.azuretools.core.mvp.model.functionapp.AzureFunctionAppMvpModel
import com.microsoft.intellij.runner.functionapp.model.FunctionAppPublishModel
import org.jetbrains.mock.FunctionAppMock
import org.jetbrains.mock.SubscriptionMock
import org.testng.annotations.AfterMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class FunctionAppConfigValidatorTest {

    @AfterMethod(alwaysRun = true)
    fun resetCacheMaps() {
        AzureFunctionAppMvpModel.clearSubscriptionIdToFunctionAppMap()
    }

    //region New App

    @Test
    fun testValidateFunctionApp_CreateNew_IsSet() {
        val mockSubscription = SubscriptionMock(subscriptionId = "test-subscription-id")
        AzureFunctionAppMvpModel.setSubscriptionIdToFunctionAppsMap(mapOf(
                mockSubscription.subscriptionId() to listOf(FunctionAppMock(name = "test-function-app"))
        ))

        val model = FunctionAppPublishModel().apply {
            isCreatingNewApp = true
            subscription = mockSubscription
            appName = "test-function-app-1"
            resourceGroupName = "test-resource-group"
            isCreatingAppServicePlan = false
            appServicePlanId = "test-app-service-plan-id"
            isCreatingStorageAccount = false
            storageAccountId = "test-storage-account-id"
        }

        FunctionAppConfigValidator.validateFunctionApp(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Subscription not provided\\.")
    fun testValidateFunctionApp_CreateNew_SubscriptionIsNotSet() {
        val model = FunctionAppPublishModel().apply {
            isCreatingNewApp = true
            subscription = null
            appName = "test-function-app"
        }

        FunctionAppConfigValidator.validateFunctionApp(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Function App name not provided\\.")
    fun testValidateFunctionApp_CreateNew_NameIsNotSet() {
        val model = FunctionAppPublishModel().apply {
            isCreatingNewApp = true
            subscription = SubscriptionMock()
            appName = ""
        }

        FunctionAppConfigValidator.validateFunctionApp(model)
    }

    @DataProvider(name = "validateFunctionAppNewResourceGroupNotSetData")
    fun validateFunctionAppNewResourceGroupNotSetData() = arrayOf(
            arrayOf("Existing", false),
            arrayOf("CreateNew", true)
    )

    @Test(dataProvider = "validateFunctionAppNewResourceGroupNotSetData",
            expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Resource Group name not provided\\.")
    fun testValidateFunctionApp_CreateNew_ResourceGroupIsNotSet(name: String, isCreatingNewResourceGroup: Boolean) {
        val mockSubscription = SubscriptionMock(subscriptionId = "test-subscription-id")
        AzureFunctionAppMvpModel.setSubscriptionIdToFunctionAppsMap(mapOf(
                mockSubscription.subscriptionId() to listOf(FunctionAppMock(name = "test-function-app"))
        ))

        val model = FunctionAppPublishModel().apply {
            isCreatingNewApp = true
            subscription = mockSubscription
            appName = "test-function-app-1"
            isCreatingResourceGroup = isCreatingNewResourceGroup
            resourceGroupName = ""
        }

        FunctionAppConfigValidator.validateFunctionApp(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "App Service Plan name not provided\\.")
    fun testValidateFunctionApp_CreateNew_AppServicePlanNameIsNotSet() {
        val mockSubscription = SubscriptionMock(subscriptionId = "test-subscription-id")
        AzureFunctionAppMvpModel.setSubscriptionIdToFunctionAppsMap(mapOf(
                mockSubscription.subscriptionId() to listOf(FunctionAppMock(name = "test-function-app"))
        ))

        val model = FunctionAppPublishModel().apply {
            isCreatingNewApp = true
            subscription = mockSubscription
            appName = "test-function-app-1"
            isCreatingResourceGroup = false
            resourceGroupName = "test-resource-group"
            isCreatingAppServicePlan = true
            appServicePlanName = ""
        }

        FunctionAppConfigValidator.validateFunctionApp(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "App Service Plan ID is not defined\\.")
    fun testValidateFunctionApp_CreateNew_AppServicePlanIsNotSet() {
        val mockSubscription = SubscriptionMock(subscriptionId = "test-subscription-id")
        AzureFunctionAppMvpModel.setSubscriptionIdToFunctionAppsMap(mapOf(
                mockSubscription.subscriptionId() to listOf(FunctionAppMock(name = "test-function-app"))
        ))

        val model = FunctionAppPublishModel().apply {
            isCreatingNewApp = true
            subscription = mockSubscription
            appName = "test-function-app-1"
            isCreatingResourceGroup = false
            resourceGroupName = "test-resource-group"
            isCreatingAppServicePlan = false
            appServicePlanId = ""
        }

        FunctionAppConfigValidator.validateFunctionApp(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Storage Account name not provided\\.")
    fun testValidateFunctionApp_CreateNew_StorageAccountNameIsNotSet() {
        val mockSubscription = SubscriptionMock(subscriptionId = "test-subscription-id")
        AzureFunctionAppMvpModel.setSubscriptionIdToFunctionAppsMap(mapOf(
                mockSubscription.subscriptionId() to listOf(FunctionAppMock(name = "test-function-app"))
        ))

        val model = FunctionAppPublishModel().apply {
            isCreatingNewApp = true
            subscription = mockSubscription
            appName = "test-function-app-1"
            isCreatingResourceGroup = false
            resourceGroupName = "test-resource-group"
            isCreatingAppServicePlan = false
            appServicePlanId = "test-app-service-plan"
            isCreatingStorageAccount = true
            storageAccountName = ""
        }

        FunctionAppConfigValidator.validateFunctionApp(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Storage Account ID not defined\\.")
    fun testValidateFunctionApp_CreateNew_StorageAccountIsNotSet() {
        val mockSubscription = SubscriptionMock(subscriptionId = "test-subscription-id")
        AzureFunctionAppMvpModel.setSubscriptionIdToFunctionAppsMap(mapOf(
                mockSubscription.subscriptionId() to listOf(FunctionAppMock(name = "test-function-app"))
        ))

        val model = FunctionAppPublishModel().apply {
            isCreatingNewApp = true
            subscription = mockSubscription
            appName = "test-function-app-1"
            isCreatingResourceGroup = false
            resourceGroupName = "test-resource-group"
            isCreatingAppServicePlan = false
            appServicePlanId = "test-app-service-plan"
            isCreatingStorageAccount = false
            storageAccountId = ""
        }

        FunctionAppConfigValidator.validateFunctionApp(model)
    }

    //endregion New App

    //region Existing App

    @Test
    fun testValidateFunctionApp_ExistingApp_IsSet() {
        val model = FunctionAppPublishModel().apply {
            isCreatingNewApp = false
            appId = "test-function-app-id"
            subscription = SubscriptionMock()
        }

        FunctionAppConfigValidator.validateFunctionApp(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Please select an existing Function App\\.")
    fun testValidateFunctionApp_ExistingApp_AppNotSet() {
        val model = FunctionAppPublishModel().apply {
            isCreatingNewApp = false
            appId = ""
        }

        FunctionAppConfigValidator.validateFunctionApp(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Subscription not provided\\.")
    fun testValidateFunctionApp_ExistingApp_SubscriptionNotSet() {
        val model = FunctionAppPublishModel().apply {
            isCreatingNewApp = false
            appId = "test-function-app-id"
            subscription = null
        }

        FunctionAppConfigValidator.validateFunctionApp(model)
    }

    //endregion Existing App
}
