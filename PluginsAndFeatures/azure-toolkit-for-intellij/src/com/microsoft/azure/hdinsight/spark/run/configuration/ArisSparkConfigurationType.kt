/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.microsoft.azure.hdinsight.common.CommonConst
import com.microsoft.azure.hdinsight.common.IconPathBuilder
import com.microsoft.intellij.util.PluginUtil
import javax.swing.Icon

class ArisSparkConfigurationType : ConfigurationType {
    companion object {
        @JvmStatic
        val instance by lazy { ConfigurationTypeUtil.findConfigurationType(ArisSparkConfigurationType::class.java) }
    }
    override fun getIcon(): Icon {
        // TODO: should use Aris config icon
        return PluginUtil.getIcon(IconPathBuilder
                .custom(CommonConst.OpenSparkUIIconName)
                .build())
    }

    override fun getDisplayName(): String {
        return "Aris On Spark"
    }

    override fun getId(): String {
        return "ArisOnSparkConfiguration"
    }

    override fun getConfigurationTypeDescription(): String {
        return "Aris On Spark Configuration"
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(ArisSparkConfigurationFactory(this))
    }
}
