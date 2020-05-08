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

import com.jetbrains.rider.test.asserts.shouldBe
import com.jetbrains.rider.test.asserts.shouldBeEmpty
import com.jetbrains.rider.test.asserts.shouldBeFalse
import com.jetbrains.rider.test.asserts.shouldBeTrue
import com.microsoft.azuretools.utils.AzureModel
import org.jetbrains.mock.AppServicePlanMock
import org.jetbrains.mock.ResourceGroupMock
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.testng.annotations.AfterMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class AppServicePlanValidatorTest {

    companion object {
        private val originalResourceGroupToAppServicePlanMap = AzureModel.getInstance().resourceGroupToAppServicePlanMap
    }

    @AfterMethod(alwaysRun = true)
    fun resetCacheMaps() {
        AzureModel.getInstance().resourceGroupToAppServicePlanMap = originalResourceGroupToAppServicePlanMap
    }

    //region Plan

    @Test
    fun testCheckAppServicePlanIsSet_IsSet() {
        val validationResult = AppServicePlanValidator.checkAppServicePlanIsSet(AppServicePlanMock())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckAppServicePlanIsSet_IsNotSet() {
        val validationResult = AppServicePlanValidator.checkAppServicePlanIsSet(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.service_plan.not_defined"))
    }

    //endregion Plan

    //region Plan ID

    @Test
    fun testCheckAppServicePlanIdIsSet_IsSet() {
        val validationResult = AppServicePlanValidator.checkAppServicePlanIdIsSet("test-plan")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckAppServicePlanIdIsSet_IsNotSet() {
        val validationResult = AppServicePlanValidator.checkAppServicePlanIdIsSet(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.service_plan.id_not_defined"))
    }

    @Test
    fun testCheckAppServicePlanIdIsSet_IsEmpty() {
        val validationResult = AppServicePlanValidator.checkAppServicePlanIdIsSet("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.service_plan.id_not_defined"))
    }

    //endregion Plan ID

    //region Plan Name

    @Test
    fun testCheckAppServicePlanNameIsSet_IsSet() {
        val validationResult = AppServicePlanValidator.checkAppServicePlanNameIsSet("app-service-plan-name")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckAppServicePlanNameIsSet_IsEmpty() {
        val validationResult = AppServicePlanValidator.checkAppServicePlanNameIsSet("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.service_plan.name_not_defined"))
    }

    //endregion Plan Name

    //region Name Max Length

    @Test
    fun testCheckAppServicePlanNameMaxLength_BelowMax() {
        val validationResult = AppServicePlanValidator.checkAppServicePlanNameMaxLength("a".repeat(39))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckAppServicePlanNameMaxLength_Max() {
        val validationResult = AppServicePlanValidator.checkAppServicePlanNameMaxLength("a".repeat(40))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckAppServicePlanNameMaxLength_AboveMax() {
        val validationResult = AppServicePlanValidator.checkAppServicePlanNameMaxLength("a".repeat(41))

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.service_plan.name_length_error", 1, 40))
    }

    //endregion Name Max Length

    //region Name Min Length

    @Test
    fun testCheckAppServicePlanNameMinLength_BelowMin() {
        val validationResult = AppServicePlanValidator.checkAppServicePlanNameMinLength("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.service_plan.name_length_error", 1, 40))
    }

    @Test
    fun testCheckAppServicePlanNameMinLength_Min() {
        val validationResult = AppServicePlanValidator.checkAppServicePlanNameMinLength("a")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckAppServicePlanNameMinLength_AboveMin() {
        val validationResult = AppServicePlanValidator.checkAppServicePlanNameMinLength("a".repeat(2))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Name Min Length

    //region Name Exists

    @DataProvider(name = "checkAppServicePlanExistsData")
    fun checkAppServicePlanExistsData() = arrayOf(
            arrayOf("ExactMatch", "test-app-service-plan"),
            arrayOf("Case", "TEST-app-SERVICE-plan")
    )

    @Test(dataProvider = "checkAppServicePlanExistsData")
    fun testCheckAppServicePlanNameExists_Exists(name: String, planName: String) {
        AzureModel.getInstance().resourceGroupToAppServicePlanMap = mapOf(
                ResourceGroupMock() to listOf(AppServicePlanMock(planName.toLowerCase()))
        )

        val validationResult = AppServicePlanValidator.checkAppServicePlanNameExists(planName)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.service_plan.already_exists", planName))
    }

    @Test
    fun testCheckAppServicePlanNameExists_NotExist() {
        AzureModel.getInstance().resourceGroupToAppServicePlanMap = mapOf(
                ResourceGroupMock() to listOf(AppServicePlanMock(name = "test-app-service-plan"))
        )

        val validationResult = AppServicePlanValidator.checkAppServicePlanNameExists("test-app-service-plan-1")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Name Exists

    //region Invalid Characters

    @DataProvider(name = "planNameInvalidCharactersData")
    fun planNameInvalidCharactersData() = arrayOf(
            arrayOf("NonAlphabeticChar", "£∂®-app-¢≥", arrayOf('£', '∂', '®', '¢', '≥')),
            arrayOf("Symbols", "test/plan_1@2^", arrayOf('/', '_', '@', '^')),
            arrayOf("Space", "app service plan", arrayOf(' ')),
            arrayOf("Nbsp", "app${'\u00A0'}service${'\u00A0'}plan", arrayOf('\u00A0'))
    )

    @Test(dataProvider = "planNameInvalidCharactersData")
    fun testCheckInvalidCharacters_InvalidCharacters(name: String, planName: String, invalidChars: Array<Char>) {
        val invalidCharsString = invalidChars.joinToString("', '", "'", "'")

        val validationResult = AppServicePlanValidator.checkInvalidCharacters(planName)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("${RiderAzureBundle.message("run_config.publish.validation.service_plan.name_invalid")} $invalidCharsString.")
    }

    @DataProvider(name = "planNameValidCharactersData")
    fun planNameValidCharactersData() = arrayOf(
            arrayOf("NonAlphabeticChar", "å-name-ƒ"),
            arrayOf("NonEnglishAlphabeticChar", "mixed-язык-plan")
    )

    @Test(dataProvider = "planNameValidCharactersData")
    fun testCheckInvalidCharacters_ValidCharacters(name: String, planName: String) {
        val validationResult = AppServicePlanValidator.checkInvalidCharacters(planName)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Invalid Characters

    //region Validate Service Plan Name

    @Test
    fun testValidateAppServicePlanName_ValidName() {
        val validationResult = AppServicePlanValidator.validateAppServicePlanName("test-plan-123")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testValidateAppServicePlanName_PlanNameIsNotSet() {
        val validationResult = AppServicePlanValidator.validateAppServicePlanName("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.service_plan.name_not_defined"))
    }

    @Test
    fun testValidateAppServicePlanName_PlanAlreadyExists() {
        AzureModel.getInstance().resourceGroupToAppServicePlanMap = mapOf(
                ResourceGroupMock() to listOf(AppServicePlanMock())
        )

        val validationResult = AppServicePlanValidator.validateAppServicePlanName("test-app-service-plan")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.service_plan.already_exists", "test-app-service-plan"))
    }

    @Test
    fun testValidateAppServicePlanName_MultipleErrors() {
        val validationResult = AppServicePlanValidator.validateAppServicePlanName("-this_is_™[plan.to,check".repeat(2))
        val invalidCharsString = arrayOf('_', '™', '[', '.', ',').joinToString("', '", "'", "'")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(2)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.service_plan.name_length_error", 1, 40))
        validationResult.errors[1].shouldBe("${RiderAzureBundle.message("run_config.publish.validation.service_plan.name_invalid")} $invalidCharsString."
        )
    }

    //endregion Validate Service Plan Name
}
