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
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class AppValidatorTest {

    companion object {
        private const val TEST_APP_TYPE = "Test App"
    }

    private val validator = AppValidator(TEST_APP_TYPE)

    //region App ID

    @Test
    fun testCheckAppIdIsSet_IsSet() {
        val validationResult = validator.checkAppIdIsSet("test_id")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckAppIdIsSet_IsNotSet() {
        val validationResult = validator.checkAppIdIsSet(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.app.not_defined", TEST_APP_TYPE))
    }

    @Test
    fun testCheckAppIdIsSet_IsEmpty() {
        val validationResult = validator.checkAppIdIsSet("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.app.not_defined", TEST_APP_TYPE))
    }

    //endregion App ID

    //region App Name

    @Test
    fun testCheckAppNameIsSet_IsSet() {
        val validationResult = validator.checkAppNameIsSet("app-name")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckAppNameIsSet_IsEmpty() {
        val validationResult = validator.checkAppNameIsSet("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.app.name_not_defined", TEST_APP_TYPE))
    }

    //endregion App Name

    //region App Starts Ends With Dash

    @Test
    fun testCheckAppNameIsSet_NotStartsAndEndsWithDashes() {
        val validationResult = validator.checkStartsEndsWithDash("test-name")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @DataProvider(name = "checkStartsEndsWithDashData")
    fun checkStartsEndsWithDashData() = arrayOf(
            arrayOf("StartsWithDash", "-test-name"),
            arrayOf("EndsWithDash", "test-name-"),
            arrayOf("StartsEndsWithDash", "-test-name-")
    )

    @Test(dataProvider = "checkStartsEndsWithDashData")
    fun testCheckAppNameIsSet_StartsWithDash(name: String, appName: String) {
        val validationResult = validator.checkStartsEndsWithDash(appName)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.app.name_cannot_start_end_with_dash", TEST_APP_TYPE))
    }

    //endregion App Starts Ends With Dash

    //region App Min Length

    @Test
    fun testCheckNameMinLength_BelowMin() {
        val validationResult = validator.checkNameMinLength("t")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.app.name_length_error", TEST_APP_TYPE, 2, 60))
    }

    @Test
    fun testCheckNameMinLength_Min() {
        val validationResult = validator.checkNameMinLength("te")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckNameMinLength_AboveMin() {
        val validationResult = validator.checkNameMinLength("test-name")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion App Min Length

    //region App Max Length

    @Test
    fun testCheckNameMaxLength_BelowMax() {
        val validationResult = validator.checkNameMaxLength("test-name")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckNameMaxLength_Max() {
        val validationResult = validator.checkNameMaxLength("t".repeat(60))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckNameMaxLength_AboveMax() {
        val validationResult = validator.checkNameMaxLength("t".repeat(61))

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.app.name_length_error", TEST_APP_TYPE, 2, 60))
    }

    //endregion App Max Length

    //region Invalid Characters

    @DataProvider(name = "appNameInvalidCharactersData")
    fun appNameInvalidCharactersData() = arrayOf(
            arrayOf("NonAlphabeticChar", "£∂®-app-¢≥", arrayOf('£', '∂', '®', '¢', '≥')),
            arrayOf("NonSupportedSymbols", "test/app_1@2^", arrayOf('/', '_', '@', '^'))
    )

    @Test(dataProvider = "appNameInvalidCharactersData")
    fun testCheckInvalidCharacters_InvalidCharacters(name: String, appName: String, invalidChars: Array<Char>) {
        val invalidCharsString = invalidChars.joinToString("', '", "'", "'")

        val validationResult = validator.checkInvalidCharacters(appName)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("${RiderAzureBundle.message("run_config.publish.validation.app.name_invalid", TEST_APP_TYPE)} $invalidCharsString.")
    }

    @DataProvider(name = "appNameValidCharactersData")
    fun appNameValidCharactersData() = arrayOf(
            arrayOf("NonAlphabeticChar", "å-name-ƒ"),
            arrayOf("NonEnglishAlphabeticChar", "mixed-язык-app")
    )

    @Test(dataProvider = "appNameValidCharactersData")
    fun testCheckInvalidCharacters_ValidCharacters(name: String, appName: String) {
        val validationResult = validator.checkInvalidCharacters(appName)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Invalid Characters

    //region Connection String Name

    @Test
    fun testCheckConnectionStringNameIsSet_IsSet() {
        val validationResult = validator.checkConnectionStringNameIsSet("testConnection")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckConnectionStringNameIsSet_IsEmpty() {
        val validationResult = validator.checkConnectionStringNameIsSet("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.connection_string.not_defined"))
    }

    //endregion Connection String Name

    //region Validate App Name

    @Test
    fun testValidateAppName_ValidName() {
        val validationResult = validator.validateAppName("test-app-123")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testValidateAppName_AppNameIsNotSet() {
        val validationResult = validator.validateAppName("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.app.name_not_defined", TEST_APP_TYPE))
    }

    @Test
    fun testValidateAppName_MultipleErrors() {
        val validationResult = validator.validateAppName("-this_is_™[app.to,check".repeat(3))
        val invalidCharsString = arrayOf('_', '™', '[', '.', ',').joinToString("', '", "'", "'")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(3)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.app.name_cannot_start_end_with_dash", TEST_APP_TYPE))
        validationResult.errors[1].shouldBe(RiderAzureBundle.message("run_config.publish.validation.app.name_length_error", TEST_APP_TYPE, 2, 60))
        validationResult.errors[2].shouldBe("${RiderAzureBundle.message("run_config.publish.validation.app.name_invalid", TEST_APP_TYPE)} $invalidCharsString.")
    }

    //endregion Validate App Name

    //region Deployment Slot

    @Test
    fun testCheckDeploymentSlotName_IsSet() {
        val validationResult = validator.checkDeploymentSlotNameIsSet("test-deployment-slot")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckDeploymentSlotName_IsNotSet() {
        val validationResult = validator.checkDeploymentSlotNameIsSet("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("dialog.create_deployment_slot.validation.slot.name_not_defined"))
    }

    @Test
    fun testCheckDeploymentSlotName_MinLength_BelowMin() {
        val validationResult = validator.checkDeploymentSlotNameMinLength("t")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("dialog.create_deployment_slot.validation.slot.name_length_error", 2, 59))
    }

    @Test
    fun testCheckDeploymentSlotName_MinLength_Min() {
        val validationResult = validator.checkDeploymentSlotNameMinLength("te")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckDeploymentSlotName_MinLength_AboveMin() {
        val validationResult = validator.checkDeploymentSlotNameMinLength("tes")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckDeploymentSlotName_MaxLength_BelowMax() {
        val validationResult = validator.checkDeploymentSlotNameMaxLength("t".repeat(58))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckDeploymentSlotName_MaxLength_Max() {
        val validationResult = validator.checkDeploymentSlotNameMaxLength("r".repeat(59))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckDeploymentSlotName_MaxLength_AboveMax() {
        val validationResult = validator.checkDeploymentSlotNameMaxLength("t".repeat(60))

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("dialog.create_deployment_slot.validation.slot.name_length_error", 2, 59))
    }

    // Note: Use data from web app name because Deployment Slots uses the same name validation rules.
    @Test(dataProvider = "appNameInvalidCharactersData")
    fun testCheckDeploymentSlotInvalidCharacters_InvalidCharacters(name: String, slotName: String, invalidChars: Array<Char>) {
        val invalidCharsString = invalidChars.joinToString("', '", "'", "'")

        val validationResult = validator.checkDeploymentSlotNameInvalidCharacters(slotName)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("${RiderAzureBundle.message("dialog.create_deployment_slot.validation.slot.name_invalid")} $invalidCharsString.")
    }

    // Note: Use data from web app name because Deployment Slots uses the same name validation rules.
    @Test(dataProvider = "appNameValidCharactersData")
    fun testCheckDeploymentSlotInvalidCharacters_ValidCharacters(name: String, slotName: String) {
        val validationResult = validator.checkDeploymentSlotNameInvalidCharacters(slotName)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testValidateDeploymentSlotName_ValidName() {
        val validationResult = validator.validateDeploymentSlotName("test-slot-123")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testValidateDeploymentSlotName_SlotNameIsNotSet() {
        val validationResult = validator.validateDeploymentSlotName("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("dialog.create_deployment_slot.validation.slot.name_not_defined"))
    }

    @Test
    fun testValidateDeploymentSlotName_MultipleErrors() {
        val validationResult = validator.validateDeploymentSlotName("-this_is_™[slot.to,check".repeat(3))
        val invalidCharsString = arrayOf('_', '™', '[', '.', ',').joinToString("', '", "'", "'")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(2)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("dialog.create_deployment_slot.validation.slot.name_length_error", 2, 59))
        validationResult.errors[1].shouldBe("${RiderAzureBundle.message("dialog.create_deployment_slot.validation.slot.name_invalid")} $invalidCharsString.")
    }

    //endregion Deployment Slot
}
