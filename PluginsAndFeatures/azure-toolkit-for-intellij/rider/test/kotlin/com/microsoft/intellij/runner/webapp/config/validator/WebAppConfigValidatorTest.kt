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

package com.microsoft.intellij.runner.webapp.config.validator

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.microsoft.azuretools.utils.AzureModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import com.microsoft.intellij.runner.webapp.model.WebAppPublishModel
import org.jetbrains.mock.*
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class WebAppConfigValidatorTest {

    //region New Web App

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Subscription not provided\\.")
    fun testValidateWebApp_SubscriptionIsNotSet() {
        val model = WebAppPublishModel().apply {
            isCreatingNewApp = true
            subscription = null
        }
        WebAppConfigValidator.validateWebApp(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Web App name not provided\\.")
    fun testValidateWebApp_NameIsNotSet() {
        val model = WebAppPublishModel().apply {
            isCreatingNewApp = true
            subscription = SubscriptionMock()
            appName = ""
        }
        WebAppConfigValidator.validateWebApp(model)
    }

    @DataProvider(name = "validateWebAppResourceGroupIsNotSetData")
    fun validateWebAppResourceGroupIsNotSetData() = arrayOf(
            arrayOf("Existing", false),
            arrayOf("CreateNew", true)
    )

    @Test(dataProvider = "validateWebAppResourceGroupIsNotSetData",
            expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Resource Group name not provided\\.")
    fun testValidateWebApp_ResourceGroupIsNotSet(name: String, isCreateResourceGroup: Boolean) {
        AzureModel.getInstance().resourceGroupToWebAppMap = mapOf(
                ResourceGroupMock() to listOf(WebAppMock(name = "test-web-app"))
        )

        val model = WebAppPublishModel().apply {
            isCreatingNewApp = true
            subscription = SubscriptionMock()
            appName = "test-web-app-1"
            isCreatingResourceGroup = isCreateResourceGroup
            resourceGroupName = ""
        }
        WebAppConfigValidator.validateWebApp(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "App Service Plan ID is not defined\\.")
    fun testValidateWebApp_AppServicePlanIdIsNotSet() {
        AzureModel.getInstance().resourceGroupToWebAppMap = mapOf(
                ResourceGroupMock() to listOf(WebAppMock(name = "test-web-app"))
        )

        val model = WebAppPublishModel().apply {
            isCreatingNewApp = true
            subscription = SubscriptionMock()
            appName = "test-web-app-1"
            isCreatingResourceGroup = false
            resourceGroupName = "test-resource-group"
            isCreatingAppServicePlan = false
            appServicePlanId = ""
        }
        WebAppConfigValidator.validateWebApp(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "App Service Plan name not provided\\.")
    fun testValidateWebApp_AppServicePlanNameIsNotSet() {
        AzureModel.getInstance().resourceGroupToWebAppMap = mapOf(
                ResourceGroupMock() to listOf(WebAppMock(name = "test-web-app"))
        )

        val model = WebAppPublishModel().apply {
            isCreatingNewApp = true
            subscription = SubscriptionMock()
            appName = "test-web-app-1"
            isCreatingResourceGroup = false
            resourceGroupName = "test-resource-group"
            isCreatingAppServicePlan = true
            appServicePlanName = ""
        }
        WebAppConfigValidator.validateWebApp(model)
    }

    @Test
    fun testValidateWebApp_IsSet() {
        val mockResourceGroup = ResourceGroupMock(name = "test-resource-group")
        AzureModel.getInstance().resourceGroupToWebAppMap = mapOf(
                mockResourceGroup to listOf(WebAppMock(name = "test-web-app"))
        )

        AzureModel.getInstance().resourceGroupToAppServicePlanMap = mapOf(
                mockResourceGroup to listOf(AppServicePlanMock(name = "test-app-service-plan"))
        )

        val model = WebAppPublishModel().apply {
            isCreatingNewApp = true
            subscription = SubscriptionMock()
            appName = "test-web-app-1"
            isCreatingResourceGroup = true
            resourceGroupName = "test-resource-group-1"
            isCreatingAppServicePlan = true
            appServicePlanName = "test-app-service-plan-1"
        }
        WebAppConfigValidator.validateWebApp(model)
    }

    //endregion New Web App

    //region Existing Web App

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Please select an existing Web App\\.")
    fun testValidateWebApp_Existing_IdIsNotSet() {
        val model = WebAppPublishModel().apply {
            isCreatingNewApp = false
            appId = ""
        }
        WebAppConfigValidator.validateWebApp(model)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Subscription not provided\\.")
    fun testValidateWebApp_Existing_SubscriptionIsNotSet() {
        val model = WebAppPublishModel().apply {
            isCreatingNewApp = false
            appId = "test-web-app-id"
            subscription = null
        }
        WebAppConfigValidator.validateWebApp(model)
    }

    @Test
    fun testValidateWebApp_Existing_AppIsSet() {
        val model = WebAppPublishModel().apply {
            isCreatingNewApp = false
            appId = "test-web-app-id"
            subscription = SubscriptionMock()
        }
        WebAppConfigValidator.validateWebApp(model)
    }

    //endregion Existing Web App

    //region Connection String Existence

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Connection String with name 'testDbConnection' already exists\\.")
    fun testCheckConnectionStringNameExistence_Exists() {
        val mockWebApp = WebAppMock(id = "test-web-app-id", name = "test-web-app")
        AzureModel.getInstance().resourceGroupToWebAppMap = mapOf(
                ResourceGroupMock() to listOf(mockWebApp)
        )

        val mockConnectionString = ConnectionStringMock(name = "testDbConnection")
        AzureDotNetWebAppMvpModel.setWebAppToConnectionStringsMap(mapOf(
                mockWebApp to listOf(mockConnectionString)
        ))

        WebAppConfigValidator.checkConnectionStringNameExistence(
                name = mockConnectionString.name(), webAppId = mockWebApp.id())
    }

    @Test
    fun testCheckConnectionStringNameExistence_NotExist() {
        val mockWebApp = WebAppMock(id = "test-web-app-id", name = "test-web-app")
        AzureModel.getInstance().resourceGroupToWebAppMap = mapOf(
                ResourceGroupMock() to listOf(mockWebApp)
        )

        val mockConnectionString = ConnectionStringMock(name = "testDbConnection")
        AzureDotNetWebAppMvpModel.setWebAppToConnectionStringsMap(mapOf(
                mockWebApp to listOf(mockConnectionString)
        ))

        WebAppConfigValidator.checkConnectionStringNameExistence(
                name = "otherDbConnection", webAppId = mockWebApp.id())
    }

    //endregion Connection String Existence
}
