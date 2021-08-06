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
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.spark.ui.SparkBatchJobConfigurable
import com.microsoft.intellij.telemetry.addTelemetryListener
import javax.swing.JComponent

class LivySparkRunConfigurationSettingsEditor(private val jobConfigurable: SparkBatchJobConfigurable)
    : SettingsEditor<LivySparkBatchJobRunConfiguration>(), ILogger {
    init {
        Disposer.register(this, jobConfigurable)
    }

    private var configuration: LivySparkBatchJobRunConfiguration? = null

    override fun resetEditorFrom(livySparkBatchJobRunConfiguration: LivySparkBatchJobRunConfiguration) {
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
        livySparkBatchJobRunConfiguration.saveToSecureStore(livySparkBatchJobRunConfiguration.submitModel)

        // save configuration name as job name
        livySparkBatchJobRunConfiguration.model.submitModel.jobName = livySparkBatchJobRunConfiguration.name
    }

    override fun createEditor(): JComponent {
        // Create telemetry listener for the whole panel
        jobConfigurable.component.addTelemetryListener(jobConfigurable.serviceName)

        return jobConfigurable.component
    }

    override fun disposeEditor() {
        Disposer.dispose(this)

        super.disposeEditor()
    }
}
