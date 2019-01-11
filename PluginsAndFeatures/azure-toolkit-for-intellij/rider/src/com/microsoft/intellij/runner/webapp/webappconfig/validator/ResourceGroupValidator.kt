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

package com.microsoft.intellij.runner.webapp.webappconfig.validator

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.jetbrains.rd.util.firstOrNull
import com.microsoft.azuretools.utils.AzureModel
import com.microsoft.intellij.runner.webapp.webappconfig.UiConstants

object ResourceGroupValidator : ConfigurationValidator() {

    private val resourceGroupRegex = "[^\\p{L}0-9-.()_]".toRegex()

    private const val RESOURCE_GROUP_NAME_MIN_LENGTH = 1
    private const val RESOURCE_GROUP_NAME_MAX_LENGTH = 90
    private const val RESOURCE_GROUP_NAME_LENGTH_ERROR =
            "Resource Group name should be from $RESOURCE_GROUP_NAME_MIN_LENGTH to $RESOURCE_GROUP_NAME_MAX_LENGTH characters"

    @Throws(RuntimeConfigurationError::class)
    fun validateResourceGroupName(subscriptionId: String, name: String) {

        checkValueIsSet(name, UiConstants.RESOURCE_GROUP_NAME_NOT_DEFINED)
        checkResourceGroupExistence(subscriptionId, name)

        if (name.endsWith('.')) throw RuntimeConfigurationError(UiConstants.RESOURCE_GROUP_NAME_CANNOT_ENDS_WITH_PERIOD)

        validateResourceName(name,
                RESOURCE_GROUP_NAME_MIN_LENGTH,
                RESOURCE_GROUP_NAME_MAX_LENGTH,
                RESOURCE_GROUP_NAME_LENGTH_ERROR,
                resourceGroupRegex,
                UiConstants.RESOURCE_GROUP_NAME_INVALID)
    }

    @Throws(RuntimeConfigurationError::class)
    private fun checkResourceGroupExistence(subscriptionId: String, resourceGroupName: String) {
        val subscriptionToResourceGroupMap = AzureModel.getInstance().subscriptionToResourceGroupMap

        if (subscriptionToResourceGroupMap != null) {
            val resourceGroups = subscriptionToResourceGroupMap.filter { it.key.subscriptionId == subscriptionId }.firstOrNull()?.value
            if (resourceGroups != null && resourceGroups.any { it.name().equals(resourceGroupName, true) })
                throw RuntimeConfigurationError(String.format(UiConstants.RESOURCE_GROUP_ALREADY_EXISTS, resourceGroupName))
        }
    }
}