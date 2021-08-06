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
import com.microsoft.intellij.helpers.AzureIconLoader
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol
import javax.swing.Icon

object ArisSparkConfigurationType : ConfigurationType {
    override fun getIcon(): Icon {
        // TODO: should use Aris config icon
        return AzureIconLoader.loadIcon(AzureIconSymbol.SQLServerBigDataCluster.MODULE)
    }

    override fun getDisplayName(): String {
        return "Apache Spark on SQL Server Big Data Cluster"
    }

    override fun getId(): String {
        return "ArisOnSparkConfiguration"
    }

    override fun getConfigurationTypeDescription(): String {
        return "Apache Spark on SQL Server Big Data Cluster Run Configuration"
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(ArisSparkConfigurationFactory(this))
    }
}
