/**
 * Copyright (c) 2020 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.helpers.validator

import com.jetbrains.rider.test.asserts.shouldBe
import com.jetbrains.rider.test.asserts.shouldBeEmpty
import com.jetbrains.rider.test.asserts.shouldBeFalse
import com.jetbrains.rider.test.asserts.shouldBeTrue
import com.microsoft.azuretools.utils.AzureModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import org.jetbrains.mock.ConnectionStringMock
import org.jetbrains.mock.DeploymentSlotMock
import org.jetbrains.mock.ResourceGroupMock
import org.jetbrains.mock.WebAppMock
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.testng.annotations.AfterMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class WebAppValidatorTest {

    companion object {
        private val originalResourceGroupToWebAppMap = AzureModel.getInstance().resourceGroupToWebAppMap
    }

    @AfterMethod(alwaysRun = true)
    fun resetCacheMaps() {
        AzureModel.getInstance().resourceGroupToWebAppMap = originalResourceGroupToWebAppMap
        AzureDotNetWebAppMvpModel.clearSubscriptionToWebAppMap()
        AzureDotNetWebAppMvpModel.clearWebAppToConnectionStringsMap()
    }

    //region Exists

    @DataProvider(name = "checkWebAppExistsData")
    fun checkWebAppExistsData() = arrayOf(
            arrayOf("ExactMatch", "test-web-app"),
            arrayOf("Case", "TEST-web-APP")
    )

    @Test(dataProvider = "checkWebAppExistsData")
    fun testCheckWebAppExists_Exists(name: String, appName: String) {
        AzureModel.getInstance().resourceGroupToWebAppMap = mapOf(
                ResourceGroupMock() to listOf(WebAppMock(name = appName.toLowerCase()))
        )

        val validationResult = WebAppValidator.checkWebAppExists(appName)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.web_app.name_already_exists", appName))
    }

    @Test
    fun testCheckWebAppExists_NotExist() {
        AzureModel.getInstance().resourceGroupToWebAppMap = mapOf(
                ResourceGroupMock() to listOf(WebAppMock(name = "test-web-app"))
        )

        val validationResult = WebAppValidator.checkWebAppExists("test-web-app-1")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Exists

    //region Connection String

    @Test
    fun testCheckConnectionStringNameExistence_ConnectionStringIsNotSet() {
        val validationResult =
                WebAppValidator.checkConnectionStringNameExistence(name = "", webAppId = "test-web-app-id")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.connection_string.not_defined"))
    }

    @Test
    fun testCheckConnectionStringNameExistence_MapIsNotDefined() {
        AzureModel.getInstance().resourceGroupToWebAppMap = null

        val validationResult =
                WebAppValidator.checkConnectionStringNameExistence(name = "testDbConnection", webAppId = "test-web-app-id")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @DataProvider(name = "checkConnectionStringNameExistWebAppNotExistsData")
    fun checkConnectionStringNameExistWebAppNotExistsData() = arrayOf(
            arrayOf("SimpleId", "test-web-app-id-1"),
            arrayOf("CaseSensitive", "TEST-WEB-APP-ID")
    )

    @Test(dataProvider = "checkConnectionStringNameExistWebAppNotExistsData")
    fun testCheckConnectionStringNameExistence_WebAppNotExist(name: String, appId: String) {
        AzureModel.getInstance().resourceGroupToWebAppMap = mapOf(
                ResourceGroupMock() to listOf(WebAppMock(id = "test-web-app-id", name = "test-web-app"))
        )

        val validationResult =
                WebAppValidator.checkConnectionStringNameExistence(name = "testDbConnection", webAppId = appId)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @DataProvider(name = "checkConnectionStringNameExistConnectionStringNotExistData")
    fun checkConnectionStringNameExistConnectionStringNotExistData() = arrayOf(
            arrayOf("NotExist", "testDbConnectionOther"),
            arrayOf("CaseSensitive", "testdbconnection")
    )

    @Test(dataProvider = "checkConnectionStringNameExistConnectionStringNotExistData")
    fun testCheckConnectionStringNameExistence_ConnectionStringNotExist(name: String, connectionStringName: String) {
        val mockWebApp = WebAppMock(id = "test-web-app-id", name = "test-web-app")
        AzureModel.getInstance().resourceGroupToWebAppMap = mapOf(
                ResourceGroupMock() to listOf(mockWebApp)
        )

        val mockConnectionString = ConnectionStringMock(name = "testDbConnection")
        AzureDotNetWebAppMvpModel.setWebAppToConnectionStringsMap(mapOf(
                mockWebApp to listOf(mockConnectionString)
        ))

        val validationResult =
                WebAppValidator.checkConnectionStringNameExistence(name = connectionStringName, webAppId = mockWebApp.id())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckConnectionStringNameExistence_ConnectionStringExists() {
        val mockWebApp = WebAppMock(id = "test-web-app-id", name = "test-web-app")
        AzureModel.getInstance().resourceGroupToWebAppMap = mapOf(
                ResourceGroupMock() to listOf(mockWebApp)
        )

        val mockConnectionString = ConnectionStringMock(name = "testDbConnection")
        AzureDotNetWebAppMvpModel.setWebAppToConnectionStringsMap(mapOf(
                mockWebApp to listOf(mockConnectionString)
        ))

        val validationResult =
                WebAppValidator.checkConnectionStringNameExistence(name = mockConnectionString.name(), webAppId = mockWebApp.id())

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.connection_string.name_already_exists", mockConnectionString.name()))
    }

    //endregion Connection String

    //region Validate Web App Name

    @Test
    fun testValidateWebAppName_IsSet() {
        val mockWebApp = WebAppMock(name = "test-web-app")
        AzureModel.getInstance().resourceGroupToWebAppMap = mapOf(
                ResourceGroupMock() to listOf(mockWebApp)
        )

        val validationResult = WebAppValidator.validateWebAppName("test-web-app-1")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testValidateWebAppName_IsNotSet() {
        val validationResult = WebAppValidator.validateWebAppName("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.app.name_not_defined", "Web App"))
    }

    @Test
    fun testValidateWebAppName_Exists() {
        val mockWebApp = WebAppMock(name = "test-web-app")
        AzureModel.getInstance().resourceGroupToWebAppMap = mapOf(
                ResourceGroupMock() to listOf(mockWebApp)
        )

        val validationResult = WebAppValidator.validateWebAppName(mockWebApp.name())

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.web_app.name_already_exists", mockWebApp.name()))
    }

    //endregion Validate Web App Name

    //region Deployment Slots

    @Test
    fun testCheckDeploymentSlot_IsNotSet() {
        val validationResult = WebAppValidator.checkDeploymentSlotIsSet(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("dialog.create_deployment_slot.validation.slot.not_defined"))
    }

    @Test
    fun testCheckDeploymentSlot_IsSet() {
        val validationResult = WebAppValidator.checkDeploymentSlotIsSet(DeploymentSlotMock())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckDeploymentSlotExists_MapIsNotDefined() {
        val mockWebApp = WebAppMock()
        val validationResult = WebAppValidator.checkDeploymentSlotNameExists(mockWebApp, "test-slot")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckDeploymentSlotExists_SlotExists() {
        val mockWebApp = WebAppMock()
        val mockDeploymentSlot = DeploymentSlotMock()
        AzureDotNetWebAppMvpModel.setWebAppToDeploymentSlotsMap(mapOf(
                mockWebApp to listOf(mockDeploymentSlot)
        ))

        val validationResult = WebAppValidator.checkDeploymentSlotNameExists(app = mockWebApp, name = mockDeploymentSlot.name())

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("dialog.create_deployment_slot.validation.slot.already_exists", mockWebApp.name(), mockDeploymentSlot.name()))
    }

    @Test
    fun testCheckDeploymentSlotExists_SlotNotExists() {
        val mockWebApp = WebAppMock()
        val mockDeploymentSlot = DeploymentSlotMock()
        AzureDotNetWebAppMvpModel.setWebAppToDeploymentSlotsMap(mapOf(
                mockWebApp to listOf(mockDeploymentSlot)
        ))

        val validationResult = WebAppValidator.checkDeploymentSlotNameExists(app = mockWebApp, name = "another-deployment-slot")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Deployment Slots
}
