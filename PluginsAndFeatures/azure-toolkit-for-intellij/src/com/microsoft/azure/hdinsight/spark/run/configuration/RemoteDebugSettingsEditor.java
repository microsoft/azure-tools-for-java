/**
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
 */

package com.microsoft.azure.hdinsight.spark.run.configuration;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel;
import com.microsoft.azure.hdinsight.spark.ui.SparkBatchJobConfigurable;
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionContentPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class RemoteDebugSettingsEditor extends SettingsEditor<RemoteDebugRunConfiguration> {
    private SparkBatchJobConfigurable jobConfigurable;

    private RemoteDebugRunConfiguration runConfiguration;

    public RemoteDebugRunConfiguration getRunConfiguration() {
        return runConfiguration;
    }

    public RemoteDebugSettingsEditor(final RemoteDebugRunConfiguration runConfiguration){
        this.runConfiguration = runConfiguration;
    }

    @Override
    protected void resetEditorFrom(@NotNull RemoteDebugRunConfiguration remoteDebugRunConfiguration) {
        // Reset the panel from the RunConfiguration
        jobConfigurable.setData(remoteDebugRunConfiguration.getModel());
    }

    @Override
    protected void applyEditorTo(@NotNull RemoteDebugRunConfiguration remoteDebugRunConfiguration) throws ConfigurationException {
        // Apply the panel's setting to RunConfiguration
        jobConfigurable.getData(remoteDebugRunConfiguration.getModel());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        jobConfigurable = new SparkBatchJobConfigurable(runConfiguration.getSubmitModel().getProject());

        return jobConfigurable.getComponent();
    }
}
