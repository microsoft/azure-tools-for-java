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

package com.microsoft.intellij.helpers.validator

import com.jetbrains.rider.test.asserts.*
import com.microsoft.azuretools.core.mvp.model.functionapp.AzureFunctionAppMvpModel
import org.jetbrains.mock.ConnectionStringMock
import org.jetbrains.mock.FunctionAppMock
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.testng.annotations.AfterMethod
import org.testng.annotations.Test

class FunctionAppValidatorTest {

    @AfterMethod(alwaysRun = true)
    fun resetCacheMaps() {
        AzureFunctionAppMvpModel.clearSubscriptionIdToFunctionAppMap()
    }

    //region Exists

    @Test
    fun testCheckFunctionAppExists_Exists() {
        val mockFunctionApp = FunctionAppMock(name = "test-function-app")
        AzureFunctionAppMvpModel.setSubscriptionIdToFunctionAppsMap(mapOf(
                "test-subscription" to listOf(mockFunctionApp)
        ))

        val validationResult = FunctionAppValidator.checkFunctionAppExists(
                subscriptionId = "test-subscription", name = mockFunctionApp.name())

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.function_app.name_already_exists", mockFunctionApp.name()))
    }

    @Test
    fun testCheckFunctionAppExists_NotExist() {
        AzureFunctionAppMvpModel.setSubscriptionIdToFunctionAppsMap(mapOf(
                "test-subscription" to listOf(FunctionAppMock(name = "test-function-app"))
        ))

        val validationResult = FunctionAppValidator.checkFunctionAppExists(
                subscriptionId = "test-subscription", name = "test-function-app-1")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Exists

    //region Connection String

    @Test
    fun testCheckConnectionStringNameExistence_IsNotSet() {
        val validationResult = FunctionAppValidator.checkConnectionStringNameExistence(
                subscriptionId = "", appId = "", connectionStringName = "")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.connection_string.not_defined"))
    }

    @Test
    fun testCheckConnectionStringNameExistence_Exists() {
        val mockFunctionApp = FunctionAppMock(id = "test-function-app-id")
        AzureFunctionAppMvpModel.setSubscriptionIdToFunctionAppsMap(mapOf(
                "test-subscription" to listOf(mockFunctionApp)
        ))

        val mockConnectionString = ConnectionStringMock(name = "testDbConnection")
        AzureFunctionAppMvpModel.setAppToConnectionStringsMap(mapOf(
                mockFunctionApp to listOf(mockConnectionString)
        ))

        val validationResult = FunctionAppValidator.checkConnectionStringNameExistence(
                subscriptionId = "test-subscription", appId = mockFunctionApp.id(), connectionStringName = mockConnectionString.name())

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.connection_string.name_already_exists", mockConnectionString.name()))
    }

    @Test
    fun testCheckConnectionStringNameExistence_NotExist() {
        val mockFunctionApp = FunctionAppMock(id = "test-function-app-id")
        AzureFunctionAppMvpModel.setSubscriptionIdToFunctionAppsMap(mapOf(
                "test-subscription" to listOf(mockFunctionApp)
        ))

        val mockConnectionString = ConnectionStringMock(name = "testDbConnection")
        AzureFunctionAppMvpModel.setAppToConnectionStringsMap(mapOf(
                mockFunctionApp to listOf(mockConnectionString)
        ))

        val validationResult = FunctionAppValidator.checkConnectionStringNameExistence(
                subscriptionId = "test-subscription", appId = mockFunctionApp.id(), connectionStringName = "testDbConnectionOther")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Connection String

    //region Validate Function App Name

    @Test
    fun testValidateFunctionAppName_ValidName() {
        val mockFunctionApp = FunctionAppMock(name = "test-function-app")
        AzureFunctionAppMvpModel.setSubscriptionIdToFunctionAppsMap(mapOf(
                "test-subscription" to listOf(mockFunctionApp)
        ))

        val validationResult = FunctionAppValidator.validateFunctionAppName(
                subscriptionId = "test-subscription", name = "test-function-app-1")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testValidateFunctionAppName_NameIsNotSet() {
        val validationResult = FunctionAppValidator.validateFunctionAppName(
                subscriptionId = "test-subscription", name = "")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.app.name_not_defined", "Function App"))
    }

    @Test
    fun testValidateFunctionAppName_Exists() {
        val mockFunctionApp = FunctionAppMock(name = "test-function-app")
        AzureFunctionAppMvpModel.setSubscriptionIdToFunctionAppsMap(mapOf(
                "test-subscription" to listOf(mockFunctionApp)
        ))

        val validationResult = FunctionAppValidator.validateFunctionAppName(
                subscriptionId = "test-subscription", name = mockFunctionApp.name())

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.function_app.name_already_exists", mockFunctionApp.name()))
    }

    //endregion Validate Function App Name
}
