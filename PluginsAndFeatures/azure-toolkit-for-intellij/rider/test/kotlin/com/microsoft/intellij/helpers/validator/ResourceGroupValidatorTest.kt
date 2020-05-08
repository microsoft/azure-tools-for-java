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
import org.jetbrains.mock.ResourceGroupMock
import org.jetbrains.mock.SubscriptionDetailMock
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.testng.annotations.AfterMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class ResourceGroupValidatorTest {

    companion object {
        private val originalResourceGroupToWebAppMap = AzureModel.getInstance().resourceGroupToWebAppMap
    }

    @AfterMethod(alwaysRun = true)
    fun resetCacheMaps() {
        AzureModel.getInstance().resourceGroupToWebAppMap = originalResourceGroupToWebAppMap
    }

    //region Resource Group

    @Test
    fun testCheckResourceGroupIsSet_IsSet() {
        val validationResult = ResourceGroupValidator.checkResourceGroupIsSet(ResourceGroupMock())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckResourceGroupIsSet_IsNotSet() {
        val validationResult = ResourceGroupValidator.checkResourceGroupIsSet(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.resource_group.not_defined"))
    }

    //endregion Resource Group

    //region Name

    @Test
    fun testCheckResourceGroupNameIsSet_IsSet() {
        val validationResult = ResourceGroupValidator.checkResourceGroupNameIsSet("test-resource-group")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckResourceGroupNameIsSet_IsEmpty() {
        val validationResult = ResourceGroupValidator.checkResourceGroupNameIsSet("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.resource_group.name_not_defined"))
    }

    //endregion Name

    //region Name Max Length

    @Test
    fun testCheckResourceGroupNameMaxLength_BelowMax() {
        val validationResult = ResourceGroupValidator.checkResourceGroupNameMaxLength("a".repeat(89))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckResourceGroupNameMaxLength_Max() {
        val validationResult = ResourceGroupValidator.checkResourceGroupNameMaxLength("a".repeat(90))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckResourceGroupNameMaxLength_AboveMax() {
        val validationResult = ResourceGroupValidator.checkResourceGroupNameMaxLength("a".repeat(91))

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.resource_group.name_length_error", 1, 90))
    }

    //endregion Name Max Length

    //region Name Min Length

    @Test
    fun testCheckResourceGroupNameMinLength_BelowMin() {
        val validationResult = ResourceGroupValidator.checkResourceGroupNameMinLength("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.resource_group.name_length_error", 1, 90))
    }

    @Test
    fun testCheckResourceGroupNameMinLength_Min() {
        val validationResult = ResourceGroupValidator.checkResourceGroupNameMinLength("a")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckResourceGroupNameMinLength_AboveMin() {
        val validationResult = ResourceGroupValidator.checkResourceGroupNameMinLength("a".repeat(2))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Name Min Length

    //region Name Exists

    @DataProvider(name = "checkResourceGroupNameExistsData")
    fun checkResourceGroupNameExistsData() = arrayOf(
            arrayOf("ExactMatch", "test-resource-group"),
            arrayOf("IgnoreCase", "TEST-resource-GROUP")
    )

    @Test(dataProvider = "checkResourceGroupNameExistsData")
    fun testCheckResourceGroupNameExists_Exists(name: String, resourceGroupName: String) {
        val mockSubscriptionDetail = SubscriptionDetailMock.createMockSubscriptionDetail(subscriptionId = "test-subscription-id")
        val mockResourceGroup = ResourceGroupMock(name = resourceGroupName.toLowerCase())

        AzureModel.getInstance().subscriptionToResourceGroupMap = mapOf(
                mockSubscriptionDetail to listOf(mockResourceGroup)
        )

        val validationResult = ResourceGroupValidator.checkResourceGroupNameExists(
                subscriptionId = "test-subscription-id", name = resourceGroupName)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.resource_group.name_already_exists", resourceGroupName))
    }

    @Test
    fun testCheckResourceGroupNameExists_MapIsNotSet() {
        AzureModel.getInstance().subscriptionToResourceGroupMap = null

        val validationResult = ResourceGroupValidator.checkResourceGroupNameExists(
                subscriptionId = "test-subscription-id", name = "test-resource-group")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckResourceGroupNameExists_MapWithNoMatchSubscription() {
        val mockSubscriptionDetail = SubscriptionDetailMock.createMockSubscriptionDetail(subscriptionId = "test-subscription-id")
        val mockResourceGroup = ResourceGroupMock(name = "test-resource-group")

        AzureModel.getInstance().subscriptionToResourceGroupMap = mapOf(
                mockSubscriptionDetail to listOf(mockResourceGroup)
        )

        val validationResult = ResourceGroupValidator.checkResourceGroupNameExists(
                subscriptionId = "test-subscription-id-1", name = mockResourceGroup.name())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckResourceGroupNameExists_NotExist() {
        val mockSubscriptionDetail = SubscriptionDetailMock.createMockSubscriptionDetail(subscriptionId = "test-subscription-id")
        val mockResourceGroup = ResourceGroupMock(name = "test-resource-group")

        AzureModel.getInstance().subscriptionToResourceGroupMap = mapOf(
                mockSubscriptionDetail to listOf(mockResourceGroup)
        )

        val validationResult = ResourceGroupValidator.checkResourceGroupNameExists(
                subscriptionId = "test-subscription-id", name = "test-resource-group-1")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Name Exists

    //region Ends With Period

    @Test
    fun testCheckEndsWithPeriod_EndsWithPeriod() {
        val validationResult = ResourceGroupValidator.checkEndsWithPeriod("resource.group.name.")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.resource_group.name_cannot_end_with_period"))
    }

    @Test
    fun testCheckEndsWithPeriod_NotEndWithPeriod() {
        val validationResult = ResourceGroupValidator.checkEndsWithPeriod("resource.group.name")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Ends With Period

    //region Invalid Characters

    @DataProvider(name = "resourceGroupNameInvalidCharactersData")
    fun resourceGroupNameInvalidCharactersData() = arrayOf(
            arrayOf("NonAlphabeticChar", "£∂®-resource-group-¢≥", arrayOf('£', '∂', '®', '¢', '≥')),
            arrayOf("Symbols", "resource/group&name", arrayOf('/', '&')),
            arrayOf("Space", "resource group name", arrayOf(' ')),
            arrayOf("Nbsp", "resource${'\u00A0'}group${'\u00A0'}name", arrayOf('\u00A0'))
    )

    @Test(dataProvider = "resourceGroupNameInvalidCharactersData")
    fun testCheckInvalidCharacters_InvalidCharacters(name: String, resourceGroupName: String, invalidChars: Array<Char>) {
        val invalidCharsString = invalidChars.joinToString("', '", "'", "'")

        val validationResult = ResourceGroupValidator.checkInvalidCharacters(resourceGroupName)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("${RiderAzureBundle.message("run_config.publish.validation.resource_group.name_invalid")} $invalidCharsString.")
    }

    @DataProvider(name = "resourceGroupNameValidCharactersData")
    fun resourceGroupNameValidCharactersData() = arrayOf(
            arrayOf("UpperCase", "ResourceGroupName"),
            arrayOf("Numbers", "resource-group-name-123"),
            arrayOf("Underscore", "resource_group_name"),
            arrayOf("Symbols", "resource.group(name)")
    )

    @Test(dataProvider = "resourceGroupNameValidCharactersData")
    fun testCheckInvalidCharacters_ValidCharacters(name: String, resourceGroupName: String) {
        val validationResult = ResourceGroupValidator.checkInvalidCharacters(resourceGroupName)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Invalid Characters

    //region Validate Resource Group Name

    @Test
    fun testValidateResourceGroupName_ValidName() {
        val validationResult = ResourceGroupValidator.validateResourceGroupName("test-resource-group-name")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testValidateResourceGroupName_NameIsNotSet() {
        val validationResult = ResourceGroupValidator.validateResourceGroupName("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.resource_group.name_not_defined"))
    }

    @Test
    fun testValidateResourceGroupName_MultipleErrors() {
        val invalidCharsString = arrayOf('^', '/', '*', '®').joinToString("', '", "'", "'")

        val validationResult =
                ResourceGroupValidator.validateResourceGroupName("^test/resource*group®name.".repeat(4))

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(3)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.resource_group.name_cannot_end_with_period"))
        validationResult.errors[1].shouldBe(RiderAzureBundle.message("run_config.publish.validation.resource_group.name_length_error", 1, 90))
        validationResult.errors[2].shouldBe("${RiderAzureBundle.message("run_config.publish.validation.resource_group.name_invalid")} $invalidCharsString.")
    }

    //endregion Validate Resource Group Name
}
