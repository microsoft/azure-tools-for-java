package com.microsoft.intellij.runner.webapp.webappconfig.validator

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.jetbrains.rider.util.firstOrNull
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