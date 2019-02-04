/**
 * Copyright (c) 2018 JetBrains s.r.o.
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

import com.jetbrains.rd.util.firstOrNull
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azuretools.utils.AzureModel

object ResourceGroupValidator : AzureResourceValidator() {

    private val resourceGroupRegex = "[^\\p{L}0-9-.()_]".toRegex()

    private const val RESOURCE_GROUP_NOT_DEFINED = "Resource Group not provided."
    private const val RESOURCE_GROUP_NAME_NOT_DEFINED = "Resource Group name not provided."
    private const val RESOURCE_GROUP_NAME_CANNOT_ENDS_WITH_PERIOD = "Resource Group name cannot ends with period symbol."
    private const val RESOURCE_GROUP_NAME_INVALID = "Resource Group name cannot contain characters: %s."
    private const val RESOURCE_GROUP_ALREADY_EXISTS = "Resource Group with name '%s' already exists."

    private const val RESOURCE_GROUP_NAME_MIN_LENGTH = 1
    private const val RESOURCE_GROUP_NAME_MAX_LENGTH = 90
    private const val RESOURCE_GROUP_NAME_LENGTH_ERROR =
            "Resource Group name should be from $RESOURCE_GROUP_NAME_MIN_LENGTH to $RESOURCE_GROUP_NAME_MAX_LENGTH characters"

    fun validateResourceGroupName(name: String): ValidationResult {

        val status = checkResourceGroupNameIsSet(name)
        if (!status.isValid) return status

        return status
                .merge(checkEndsWithPeriod(name))
                .merge(checkNameMinLength(name))
                .merge(checkNameMaxLength(name))
                .merge(checkInvalidCharacters(name))
    }

    fun checkResourceGroupNameIsSet(name: String) =
            checkValueIsSet(name, RESOURCE_GROUP_NAME_NOT_DEFINED)

    fun checkResourceGroupIsSet(resourceGroup: ResourceGroup?) =
            checkValueIsSet(resourceGroup, RESOURCE_GROUP_NOT_DEFINED)

    fun checkInvalidCharacters(name: String) =
            validateResourceNameRegex(name, resourceGroupRegex, RESOURCE_GROUP_NAME_INVALID)

    fun checkEndsWithPeriod(name: String): ValidationResult {
        val status = ValidationResult()
        if (name.endsWith('.')) status.setInvalid(RESOURCE_GROUP_NAME_CANNOT_ENDS_WITH_PERIOD)
        return status
    }

    fun checkNameMaxLength(name: String) =
            checkNameMaxLength(name, RESOURCE_GROUP_NAME_MAX_LENGTH, RESOURCE_GROUP_NAME_LENGTH_ERROR)

    fun checkNameMinLength(name: String) =
            checkNameMinLength(name, RESOURCE_GROUP_NAME_MIN_LENGTH, RESOURCE_GROUP_NAME_LENGTH_ERROR)

    fun checkResourceGroupExistence(subscriptionId: String, name: String): ValidationResult {
        val status = ValidationResult()
        if (isResourceGroupExist(subscriptionId, name))
            status.setInvalid(String.format(RESOURCE_GROUP_ALREADY_EXISTS, name))

        return status
    }

    private fun isResourceGroupExist(subscriptionId: String, resourceGroupName: String): Boolean {
        val subscriptionToResourceGroupMap =
                AzureModel.getInstance().subscriptionToResourceGroupMap
                        ?: return false

        val resourceGroups =
                subscriptionToResourceGroupMap.filter { it.key.subscriptionId == subscriptionId }.firstOrNull()?.value
                        ?: return false

        return resourceGroups.any { it.name().equals(resourceGroupName, true) }
    }
}
