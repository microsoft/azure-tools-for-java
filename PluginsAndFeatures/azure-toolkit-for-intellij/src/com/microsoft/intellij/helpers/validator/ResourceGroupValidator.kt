package com.microsoft.intellij.helpers.validator

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.jetbrains.rider.util.firstOrNull
import com.microsoft.azuretools.utils.AzureModel

object ResourceGroupValidator : ConfigurationValidator() {

    private const val RESOURCE_GROUP_ALREADY_EXISTS = "Resource Group with name '%s' already exists"
    private const val RESOURCE_GROUP_NAME_CANNOT_ENDS_WITH_PERIOD = "Resource Group name cannot ends with period symbol"
    private const val RESOURCE_GROUP_NAME_INVALID = "Resource Group name cannot contain characters: %s"
    const val RESOURCE_GROUP_NAME_NOT_DEFINED = "Resource Group name not provided"

    private val resourceGroupRegex = "[^\\p{L}0-9-.()_]".toRegex()

    private const val RESOURCE_GROUP_NAME_MIN_LENGTH = 1
    private const val RESOURCE_GROUP_NAME_MAX_LENGTH = 90
    private const val RESOURCE_GROUP_NAME_LENGTH_ERROR =
            "Resource Group name should be from $RESOURCE_GROUP_NAME_MIN_LENGTH to $RESOURCE_GROUP_NAME_MAX_LENGTH characters"

    @Throws(RuntimeConfigurationError::class)
    fun validateResourceGroupName(subscriptionId: String, name: String) {

        checkValueIsSet(name, RESOURCE_GROUP_NAME_NOT_DEFINED)
        checkResourceGroupExistence(subscriptionId, name)

        if (name.endsWith('.')) throw RuntimeConfigurationError(RESOURCE_GROUP_NAME_CANNOT_ENDS_WITH_PERIOD)

        validateResourceName(name,
                RESOURCE_GROUP_NAME_MIN_LENGTH,
                RESOURCE_GROUP_NAME_MAX_LENGTH,
                RESOURCE_GROUP_NAME_LENGTH_ERROR,
                resourceGroupRegex,
                RESOURCE_GROUP_NAME_INVALID)
    }

    @Throws(RuntimeConfigurationError::class)
    private fun checkResourceGroupExistence(subscriptionId: String, resourceGroupName: String) {
        val subscriptionToResourceGroupMap = AzureModel.getInstance().subscriptionToResourceGroupMap

        if (subscriptionToResourceGroupMap != null) {
            val resourceGroups = subscriptionToResourceGroupMap.filter { it.key.subscriptionId == subscriptionId }.firstOrNull()?.value
            if (resourceGroups != null && resourceGroups.any { it.name().equals(resourceGroupName, true) })
                throw RuntimeConfigurationError(String.format(RESOURCE_GROUP_ALREADY_EXISTS, resourceGroupName))
        }
    }
}