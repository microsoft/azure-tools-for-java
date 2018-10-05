package com.microsoft.intellij.configuration

import com.intellij.openapi.options.ConfigurableProvider

class AzureRiderConfigurableProvider : ConfigurableProvider() {

    override fun canCreateConfigurable() = true

    override fun createConfigurable() = AzureRiderConfigurable()
}
