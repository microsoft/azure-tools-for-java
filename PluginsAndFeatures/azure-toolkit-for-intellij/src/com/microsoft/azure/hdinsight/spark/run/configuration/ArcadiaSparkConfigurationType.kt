/*
 * Copyright (c) Microsoft Corporation
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
 *
 */
package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.microsoft.azure.hdinsight.common.CommonConst
import com.microsoft.azure.hdinsight.common.IconPathBuilder
import com.microsoft.azuretools.authmanage.CommonSettings
import com.microsoft.intellij.util.PluginUtil
import javax.swing.Icon

object ArcadiaSparkConfigurationType : ConfigurationType {
    override fun getIcon(): Icon {
        // TODO: should use Arcadia config icon
        return PluginUtil.getIcon(IconPathBuilder
                .custom(CommonConst.OpenArcadiaUIIconName)
                .build())
    }

    override fun getDisplayName(): String {
        return "Apache Spark on Synapse"
    }

    override fun getId(): String {
        return "ArcadiaOnSparkConfiguration"
    }

    override fun getConfigurationTypeDescription(): String {
        return "Spark on Synapse Run Configuration"
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return when {
            CommonSettings.isProjectArcadiaFeatureEnabled -> arrayOf(ArcadiaSparkConfigurationFactory(this))
            else -> arrayOf()
        }
    }
}