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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.microsoft.azure.hdinsight.common.CallBack;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionContentPanel;
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionExDialog;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import javax.swing.*;
import java.util.List;

import static com.intellij.codeInsight.daemon.impl.HighlightInfoType.TODO;

public class RemoteDebugSettingsEditor extends SettingsEditor<RemoteDebugRunConfiguration> {
    private JPanel myPanel;
    private RemoteDebugRunConfiguration myRunConfiguration;

    public RemoteDebugSettingsEditor(final RemoteDebugRunConfiguration runConfiguration){
        myRunConfiguration = runConfiguration;
    }
    @Override
    protected void resetEditorFrom(@NotNull RemoteDebugRunConfiguration remoteDebugRunConfiguration) {

    }

    @Override
    protected void applyEditorTo(@NotNull RemoteDebugRunConfiguration remoteDebugRunConfiguration) throws ConfigurationException {

    }

    @NotNull
    @Override
    protected JComponent createEditor() {

        Project project = myRunConfiguration.getProject();
        List<IClusterDetail> cachedClusterDetails = ClusterManagerEx.getInstance().getClusterDetailsWithoutAsync(true, project);
        SparkSubmissionContentPanel submissionPanel = new SparkSubmissionContentPanel(project, cachedClusterDetails, new CallBack() {
            @Override
            public void run() {
                // TODO Add logic to resize the window and enable/disable button

            }
        });

        return submissionPanel;
    }
}
