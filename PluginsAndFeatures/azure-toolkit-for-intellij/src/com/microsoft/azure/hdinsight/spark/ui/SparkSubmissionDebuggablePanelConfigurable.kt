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

package com.microsoft.azure.hdinsight.spark.ui

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.spark.common.SparkBatchRemoteDebugJobSshAuth.SSHAuthType.UsePassword
import com.microsoft.azure.hdinsight.spark.common.SparkBatchRemoteDebugJobSshAuth.SSHAuthType.UseKeyFile
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel
import org.apache.commons.lang3.StringUtils

class SparkSubmissionDebuggablePanelConfigurable(project: Project,
                                                 submissionPanel: SparkSubmissionDebuggableContentPanel)
    : SparkSubmissionContentPanelConfigurable(project, submissionPanel) {
    private val submissionDebuggablePanel
        get() = submissionPanel as SparkSubmissionDebuggableContentPanel

    private val advancedConfigPanel
        get() = submissionDebuggablePanel.advancedConfigPanel

    private val advancedConfigCtrl = object : SparkSubmissionAdvancedConfigCtrl(advancedConfigPanel) {
        override fun getClusterNameToCheck(): String? = selectedClusterDetail?.name
    }


    override fun onClusterSelected(cluster: IClusterDetail) {
        super.onClusterSelected(cluster)

        advancedConfigCtrl.selectCluster(cluster.name)
    }

    override fun setData(data: SparkSubmitModel) {
        // Data -> Components
        super.setData(data)

        // Advanced Configuration panel
        advancedConfigPanel.setData(data.advancedConfigModel.apply { clusterName = data.clusterName })
    }

    override fun getData(data: SparkSubmitModel) {
        // Components -> Data
        super.getData(data)

        // Advanced Configuration panel
        advancedConfigPanel.getData(data.advancedConfigModel)
        data.advancedConfigModel.clusterName = selectedClusterDetail?.name
    }

    override fun validate() {
        super.validate()

        if (advancedConfigPanel.isRemoteDebugEnabled) {
            if (advancedConfigCtrl.resultMessage.isNotBlank() &&
                !advancedConfigCtrl.isCheckPassed) {
                throw RuntimeConfigurationError("Can't save the configuration since ${advancedConfigCtrl.resultMessage}")
            }

            val advModel = advancedConfigPanel.model
            when (advModel.sshAuthType) {
                UsePassword -> if (StringUtils.isBlank(advModel.sshPassword)) {
                    throw RuntimeConfigurationError("Can't save the configuration since password is blank")
                }
                UseKeyFile -> if (advModel.sshKeyFile?.exists() != true) {
                    throw RuntimeConfigurationError("Can't save the configuration since SSh key file isn't set or doesn't exist")
                }
                else -> {}
            }
        }
    }
}