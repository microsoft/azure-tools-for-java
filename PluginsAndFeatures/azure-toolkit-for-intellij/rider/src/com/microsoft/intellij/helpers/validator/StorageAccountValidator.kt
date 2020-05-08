/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

import com.microsoft.azuretools.core.mvp.model.storage.AzureStorageAccountMvpModel
import com.microsoft.azure.management.storage.StorageAccount
import com.microsoft.azure.management.storage.StorageAccountSkuType
import org.jetbrains.plugins.azure.RiderAzureBundle.message

object StorageAccountValidator : AzureResourceValidator() {

    private const val STORAGE_ACCOUNT_NAME_MIN_LENGTH = 3
    private const val STORAGE_ACCOUNT_NAME_MAX_LENGTH = 24

    private val storageAccountNameRegex = "[^a-z0-9]".toRegex()

    private val nameLengthError =
            message("run_config.publish.validation.storage_account.name_length_error",
                    STORAGE_ACCOUNT_NAME_MIN_LENGTH, STORAGE_ACCOUNT_NAME_MAX_LENGTH)

    fun validateStorageAccountName(subscriptionId: String, name: String): ValidationResult {

        val status = checkStorageAccountNameIsSet(name)
        if (!status.isValid) return status

        status.merge(checkStorageAccountNameExists(subscriptionId, name))
        if (!status.isValid) return status

        return status
                .merge(checkStorageAccountNameMinLength(name))
                .merge(checkStorageAccountNameMaxLength(name))
                .merge(checkInvalidCharacters(name))
    }

    fun checkStorageAccountIsSet(storageAccount: StorageAccount?) =
            checkValueIsSet(storageAccount, message("run_config.publish.validation.storage_account.not_defined"))

    fun checkStorageAccountIdIsSet(storageAccountId: String) =
            checkValueIsSet(storageAccountId, message("run_config.publish.validation.storage_account.id_not_defined"))

    fun checkStorageAccountNameIsSet(name: String) =
            checkValueIsSet(name, message("run_config.publish.validation.storage_account.name_not_defined"))

    fun checkStorageAccountNameMinLength(name: String) =
            checkNameMinLength(
                    name = name,
                    minLength = STORAGE_ACCOUNT_NAME_MIN_LENGTH,
                    errorMessage = nameLengthError)

    fun checkStorageAccountNameMaxLength(name: String) =
            checkNameMaxLength(
                    name = name,
                    maxLength = STORAGE_ACCOUNT_NAME_MAX_LENGTH,
                    errorMessage = nameLengthError)

    fun checkInvalidCharacters(name: String) =
            validateResourceNameRegex(
                    name = name,
                    nameRegex = storageAccountNameRegex,
                    nameInvalidCharsMessage = "${message("run_config.publish.validation.storage_account.name_invalid")} %s.")

    fun checkStorageAccountTypeIsSet(type: StorageAccountSkuType?) =
            checkValueIsSet(type, message("run_config.publish.validation.storage_account.type_not_defined"))

    fun checkStorageAccountNameExists(subscriptionId: String, name: String): ValidationResult {
        val status = ValidationResult()
        if (isStorageAccountExist(subscriptionId, name))
            return status.setInvalid(message("run_config.publish.validation.storage_account.name_already_exists", name))

        return status
    }

    private fun isStorageAccountExist(subscriptionId: String, name: String) =
            AzureStorageAccountMvpModel.isStorageAccountNameExist(
                    subscriptionId = subscriptionId, name = name, force = false)
}