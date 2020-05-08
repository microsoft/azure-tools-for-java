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
import com.microsoft.azure.management.storage.StorageAccountSkuType
import com.microsoft.azuretools.core.mvp.model.storage.AzureStorageAccountMvpModel
import org.jetbrains.mock.StorageAccountMock
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.testng.annotations.AfterMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class StorageAccountValidatorTest {

    @AfterMethod(alwaysRun = true)
    fun resetCacheMaps() {
        AzureStorageAccountMvpModel.clearStorageAccountMap()
    }

    //region Storage Account

    @Test
    fun testCheckStorageAccountIsSet_IsSet() {
        val validationResult = StorageAccountValidator.checkStorageAccountIsSet(StorageAccountMock())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckStorageAccountIsSet_IsNotSet() {
        val validationResult = StorageAccountValidator.checkStorageAccountIsSet(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.storage_account.not_defined"))
    }

    //endregion Storage Account

    //region ID

    @Test
    fun testCheckStorageAccountIdIsSet_IsSet() {
        val validationResult = StorageAccountValidator.checkStorageAccountIdIsSet("test-storage-account-id")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckStorageAccountIdIsSet_IsEmpty() {
        val validationResult = StorageAccountValidator.checkStorageAccountIdIsSet("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.storage_account.id_not_defined"))
    }

    //endregion ID

    //region Name

    @Test
    fun testCheckStorageAccountNameIsSet_IsSet() {
        val validationResult = StorageAccountValidator.checkStorageAccountNameIsSet("test-storage-account")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckStorageAccountNameIsSet_IsEmpty() {
        val validationResult = StorageAccountValidator.checkStorageAccountNameIsSet("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.storage_account.name_not_defined"))
    }

    //endregion Name

    //region Type

    @Test
    fun testCheckStorageAccountTypeIsSet_IsSet() {
        val validationResult = StorageAccountValidator.checkStorageAccountTypeIsSet(StorageAccountSkuType.STANDARD_LRS)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckStorageAccountTypeIsSet_IsNotSet() {
        val validationResult = StorageAccountValidator.checkStorageAccountTypeIsSet(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.storage_account.type_not_defined"))
    }

    //endregion Type

    //region Name Max Length

    @Test
    fun testCheckStorageAccountNameMaxLength_BelowMax() {
        val validationResult = StorageAccountValidator.checkStorageAccountNameMaxLength("a".repeat(23))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckStorageAccountNameMaxLength_Max() {
        val validationResult = StorageAccountValidator.checkStorageAccountNameMaxLength("a".repeat(24))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckStorageAccountNameMaxLength_AboveMax() {
        val validationResult = StorageAccountValidator.checkStorageAccountNameMaxLength("a".repeat(25))

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.storage_account.name_length_error", 3, 24))
    }

    //endregion Name Max Length

    //region Name Min Length

    @Test
    fun testCheckStorageAccountNameMinLength_BelowMin() {
        val validationResult = StorageAccountValidator.checkStorageAccountNameMinLength("a".repeat(2))

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.storage_account.name_length_error", 3, 24))
    }

    @Test
    fun testCheckStorageAccountNameMinLength_Min() {
        val validationResult = StorageAccountValidator.checkStorageAccountNameMinLength("a".repeat(3))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckStorageAccountNameMinLength_AboveMin() {
        val validationResult = StorageAccountValidator.checkStorageAccountNameMinLength("a".repeat(4))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Name Min Length

    //region Name Exists

    @Test
    fun testCheckStorageAccountNameExists_Exists() {
        val mockStorageAccount = StorageAccountMock(name = "teststorageaccount")
        AzureStorageAccountMvpModel.setSubscriptionIdToStorageAccountMap(mapOf(
                "test-subscription" to listOf(mockStorageAccount)
        ))

        val validationResult = StorageAccountValidator.checkStorageAccountNameExists(
                subscriptionId = "test-subscription", name = mockStorageAccount.name())

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.storage_account.name_already_exists", mockStorageAccount.name()))
    }

    @Test
    fun testCheckStorageAccountNameExists_NotExist() {
        val mockStorageAccount = StorageAccountMock(name = "teststorageaccount")
        AzureStorageAccountMvpModel.setSubscriptionIdToStorageAccountMap(mapOf(
                "test-subscription" to listOf(mockStorageAccount)
        ))

        val validationResult = StorageAccountValidator.checkStorageAccountNameExists(
                subscriptionId = "test-subscription", name = "teststorageaccount1")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Name Exists

    //region Invalid Characters

    @DataProvider(name = "storageAccountNameInvalidCharactersData")
    fun storageAccountNameInvalidCharactersData() = arrayOf(
            arrayOf("CapitalizedChar", "StorageAccountName", arrayOf('S', 'A', 'N')),
            arrayOf("Dash", "storage-account-name", arrayOf('-')),
            arrayOf("Space", "storage account name", arrayOf(' ')),
            arrayOf("Nbsp", "storage${'\u00A0'}account${'\u00A0'}name", arrayOf('\u00A0')),
            arrayOf("NonAlphabeticChar", "£∂®-sorageaccount-¢≥", arrayOf('£', '∂', '®', '-', '¢', '≥')),
            arrayOf("Symbols", "storage/account&name", arrayOf('/', '&'))
    )

    @Test(dataProvider = "storageAccountNameInvalidCharactersData")
    fun testCheckInvalidCharacters_InvalidCharacters(name: String, storageAccountName: String, invalidChars: Array<Char>) {
        val invalidCharsString = invalidChars.joinToString("', '", "'", "'")

        val validationResult = StorageAccountValidator.checkInvalidCharacters(storageAccountName)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("${RiderAzureBundle.message("run_config.publish.validation.storage_account.name_invalid")} $invalidCharsString.")
    }

    @DataProvider(name = "storageAccountNameValidCharactersData")
    fun storageAccountNameValidCharactersData() = arrayOf(
            arrayOf("LowCaseLetters", "teststorageaccount"),
            arrayOf("Numbers", "storageaccount123")
    )

    @Test(dataProvider = "storageAccountNameValidCharactersData")
    fun testCheckInvalidCharacters_ValidCharacters(name: String, storageAccountName: String) {
        val validationResult = StorageAccountValidator.checkInvalidCharacters(storageAccountName)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Invalid Characters

    //region Validate Storage Account Name

    @Test
    fun testValidateStorageAccountName_ValidName() {
        AzureStorageAccountMvpModel.setSubscriptionIdToStorageAccountMap(mapOf(
                "test-subscription" to listOf(StorageAccountMock(name = "teststorageaccount"))
        ))

        val validationResult = StorageAccountValidator.validateStorageAccountName(
                subscriptionId = "test-subscription", name = "teststorageaccount1")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testValidateStorageAccountName_NameIsNotSet() {
        val validationResult = StorageAccountValidator.validateStorageAccountName(
                subscriptionId = "test-subscription", name = "")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.storage_account.name_not_defined"))
    }

    @Test
    fun testValidateStorageAccountName_AlreadyExists() {
        val mockStorageAccount = StorageAccountMock(name = "teststorageaccount")
        AzureStorageAccountMvpModel.setSubscriptionIdToStorageAccountMap(mapOf(
                "test-subscription" to listOf(mockStorageAccount)
        ))

        val validationResult = StorageAccountValidator.validateStorageAccountName(
                subscriptionId = "test-subscription", name = mockStorageAccount.name())

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.storage_account.name_already_exists",  mockStorageAccount.name()))
    }

    @Test
    fun testValidateStorageAccountName_MultipleErrors() {
        AzureStorageAccountMvpModel.setSubscriptionIdToStorageAccountMap(mapOf(
                "test-subscription" to listOf(StorageAccountMock(name = "teststorageaccount"))
        ))

        val invalidCharsString = arrayOf('-', 'A').joinToString("', '", "'", "'")

        val validationResult = StorageAccountValidator.validateStorageAccountName(
                subscriptionId = "test-subscription", name = "test-storage-Account".repeat(3))

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(2)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.storage_account.name_length_error", 3, 24))
        validationResult.errors[1].shouldBe("${RiderAzureBundle.message("run_config.publish.validation.storage_account.name_invalid")} $invalidCharsString.")
    }

    //endregion Validate Storage Account Name
}
