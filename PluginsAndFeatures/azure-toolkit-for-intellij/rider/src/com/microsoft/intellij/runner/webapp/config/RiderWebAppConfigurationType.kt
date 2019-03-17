/**
 * Copyright (c) 2018-2019 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.webapp.config

import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.util.IconLoader
import org.jetbrains.annotations.Nls
import javax.swing.Icon

class RiderWebAppConfigurationType : ConfigurationType {

    companion object {
        private const val RUN_CONFIG_TYPE_ID = "AzureDotNetWebAppPublish"
        private const val RUN_CONFIG_TYPE_NAME = "Azure - Publish Web App"
        private const val RUN_CONFIG_TYPE_DESCRIPTION = "Azure Publish Web App configuration"
    }

    override fun getId() = RUN_CONFIG_TYPE_ID

    @Nls
    override fun getDisplayName() = RUN_CONFIG_TYPE_NAME

    @Nls
    override fun getConfigurationTypeDescription() = RUN_CONFIG_TYPE_DESCRIPTION

    override fun getIcon(): Icon = IconLoader.getIcon("icons/publishAzure.svg")

    override fun getConfigurationFactories() =
            arrayOf(RiderWebAppConfigurationFactory(this))
}
