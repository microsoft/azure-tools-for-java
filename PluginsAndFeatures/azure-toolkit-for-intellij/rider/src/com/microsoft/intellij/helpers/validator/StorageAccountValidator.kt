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

package com.microsoft.intellij.helpers.validator

import com.microsoft.azuretools.core.mvp.model.storage.AzureStorageAccountMvpModel
import com.microsoft.azure.management.storage.StorageAccount
import com.microsoft.azure.management.storage.StorageAccountSkuType

object StorageAccountValidator : AzureResourceValidator() {

    private const val STORAGE_ACCOUNT_NOT_DEFINED = "Storage Account not provided."
    private const val STORAGE_ACCOUNT_TYPE_NOT_DEFINED = "Storage Account Type not provided."
    private const val STORAGE_ACCOUNT_NAME_NOT_DEFINED = "Storage Account name not provided."
    private const val STORAGE_ACCOUNT_ID_NOT_DEFINED = "App Service Plan ID is not defined."
    private const val STORAGE_ACCOUNT_NAME_INVALID = "App Service Plan name should consists of numbers and lower case letters. Invalid characters: %s."
    private const val STORAGE_ACCOUNT_ALREADY_EXISTS = "Storage Account with name '%s' already exists."

    private val storageAccountNameRegex = "[^a-z0-9]".toRegex()

    private const val STORAGE_ACCOUNT_NAME_MIN_LENGTH = 3
    private const val STORAGE_ACCOUNT_NAME_MAX_LENGTH = 24
    private const val STORAGE_ACCOUNT_NAME_LENGTH_ERROR =
            "Storage Account name should be from $STORAGE_ACCOUNT_NAME_MIN_LENGTH to $STORAGE_ACCOUNT_NAME_MAX_LENGTH characters."

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
            checkValueIsSet(storageAccount, STORAGE_ACCOUNT_NOT_DEFINED)

    fun checkStorageAccountIdIsSet(storageAccountId: String?) =
            checkValueIsSet(storageAccountId, STORAGE_ACCOUNT_ID_NOT_DEFINED)

    fun checkStorageAccountNameIsSet(name: String) =
            checkValueIsSet(name, STORAGE_ACCOUNT_NAME_NOT_DEFINED)

    fun checkStorageAccountNameMinLength(name: String) =
            checkNameMinLength(name, STORAGE_ACCOUNT_NAME_MIN_LENGTH, STORAGE_ACCOUNT_NAME_LENGTH_ERROR)

    fun checkStorageAccountNameMaxLength(name: String) =
            checkNameMaxLength(name, STORAGE_ACCOUNT_NAME_MAX_LENGTH, STORAGE_ACCOUNT_NAME_LENGTH_ERROR)

    fun checkInvalidCharacters(name: String) =
            validateResourceNameRegex(name, storageAccountNameRegex, STORAGE_ACCOUNT_NAME_INVALID)

    fun checkStorageAccountTypeIsSet(type: StorageAccountSkuType) =
            checkValueIsSet(type, STORAGE_ACCOUNT_TYPE_NOT_DEFINED)

    fun checkStorageAccountNameExists(subscriptionId: String, name: String): ValidationResult {
        val status = ValidationResult()
        if (isStorageAccountExist(subscriptionId, name)) return status.setInvalid(String.format(STORAGE_ACCOUNT_ALREADY_EXISTS, name))
        return status
    }

    private fun isStorageAccountExist(subscriptionId: String, name: String) =
            AzureStorageAccountMvpModel.isStorageAccountNameExist(
                    subscriptionId = subscriptionId, name = name, force = false)
}