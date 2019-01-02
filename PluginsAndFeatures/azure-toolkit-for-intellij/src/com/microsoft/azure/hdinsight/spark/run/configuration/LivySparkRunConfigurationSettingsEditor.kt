/**
 * Copyright (c) Microsoft Corporation
 *
 *
 * All rights reserved.
 *
 *
 * MIT License
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.util.Disposer
import com.microsoft.azure.hdinsight.spark.common.SparkBatchRemoteDebugJobSshAuth.SSHAuthType.UsePassword
import com.microsoft.azure.hdinsight.spark.ui.SparkBatchJobConfigurable
import com.microsoft.azuretools.securestore.SecureStore
import com.microsoft.azuretools.service.ServiceManager
import javax.swing.JComponent

class LivySparkRunConfigurationSettingsEditor(val jobConfigurable: SparkBatchJobConfigurable) : SettingsEditor<LivySparkBatchJobRunConfiguration>() {
    init {
        Disposer.register(this, jobConfigurable)
    }

    private val secureStore: SecureStore? = ServiceManager.getServiceProvider(SecureStore::class.java)

    private var configuration: LivySparkBatchJobRunConfiguration? = null

    override fun resetEditorFrom(livySparkBatchJobRunConfiguration: LivySparkBatchJobRunConfiguration) {
        val advModel = livySparkBatchJobRunConfiguration.submitModel.advancedConfigModel

        if (advModel.enableRemoteDebug && advModel.sshAuthType == UsePassword) {
            // Load password for no password input
            try {
                advModel.clusterName = livySparkBatchJobRunConfiguration.submitModel.clusterName
                advModel.sshPassword = secureStore?.loadPassword(advModel.credentialStoreAccount, advModel.sshUserName)
            } catch (ignored: Exception) { }
        }

        // Reset the panel from the RunConfiguration
        jobConfigurable.setData(livySparkBatchJobRunConfiguration.model)

        // Remember the setting source configuration
        configuration = livySparkBatchJobRunConfiguration
    }

    @Throws(ConfigurationException::class)
    override fun applyEditorTo(livySparkBatchJobRunConfiguration: LivySparkBatchJobRunConfiguration) {
        // Apply the panel's setting to RunConfiguration
        jobConfigurable.validateInputs()
        jobConfigurable.getData(livySparkBatchJobRunConfiguration.model)

        if (livySparkBatchJobRunConfiguration != configuration) {
            // The code path getSnapshot()'s configuration is different than saved setting source
            return
        }

        // When click 'OK' or 'Apply' buttons, the saved setting source run configuration is used
        val advModel = livySparkBatchJobRunConfiguration.submitModel.advancedConfigModel

        if (advModel.enableRemoteDebug && advModel.sshAuthType == UsePassword && !advModel.sshPassword.isNullOrBlank()) {
            secureStore?.savePassword(advModel.credentialStoreAccount, advModel.sshUserName, advModel.sshPassword)
        }
    }

    override fun createEditor(): JComponent {
        return jobConfigurable.component
    }

    override fun disposeEditor() {
        Disposer.dispose(this)

        super.disposeEditor()
    }
}
