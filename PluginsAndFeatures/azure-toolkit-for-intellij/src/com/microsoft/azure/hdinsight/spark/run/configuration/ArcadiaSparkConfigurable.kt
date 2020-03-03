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

import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.spark.ui.SparkBatchJobConfigurable
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionContentPanel
import com.microsoft.azuretools.authmanage.CommonSettings
import com.microsoft.azuretools.telemetry.TelemetryConstants
import javax.swing.JComponent
import javax.swing.JLabel

class ArcadiaSparkConfigurable(project: Project) : SparkBatchJobConfigurable(project) {
    override fun getComponent(): JComponent {
        if (CommonSettings.isProjectArcadiaFeatureEnabled) {
            return super.getComponent()
        }

        return JLabel("The feature is coming soon.");
    }

    override fun createSubmissionPanel(): SparkSubmissionContentPanel =
            ArcadiaSparkSubmissionContentPanel(project)

    override fun getServiceName(): String {
        return TelemetryConstants.SPARK_ON_ARCADIA
    }
}