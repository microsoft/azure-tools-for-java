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

package com.microsoft.intellij.configuration

import com.intellij.application.options.OptionsContainingConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.microsoft.intellij.configuration.ui.AzureAppServicesConfigurationPanel
import com.microsoft.intellij.configuration.ui.AzureFunctionsConfigurationPanel

class AzureRiderConfigurable :
        SearchableConfigurable.Parent.Abstract(), OptionsContainingConfigurable {

    companion object {
        private const val AZURE_CONFIGURATION_ID = "com.intellij"
        private const val AZURE_CONFIGURATION_NAME = "Azure"
    }

    private var myPanels = listOf<Configurable>()

    override fun getId() = AZURE_CONFIGURATION_ID

    override fun getDisplayName() = AZURE_CONFIGURATION_NAME

    override fun buildConfigurables(): Array<Configurable> {
        val panels = listOf<Configurable>(
                AzureRiderAbstractConfigurable(AzureAppServicesConfigurationPanel()),
                AzureRiderAbstractConfigurable(AzureFunctionsConfigurationPanel())
        )
        myPanels = panels
        return panels.toTypedArray()
    }

    override fun processListOptions() = hashSetOf<String>()
}