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
 */

package com.microsoft.azure.hdinsight.spark.run.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobRemoteProcess;
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobRunProcessHandler;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Optional;

public class SparkBatchJobDisconnectAction extends AnAction {
    @Nullable
    private SparkBatchJobRemoteProcess remoteProcess;
    private boolean isEnabled = true;

    public SparkBatchJobDisconnectAction() {
        super();
    }

    public SparkBatchJobDisconnectAction(@Nullable SparkBatchJobRemoteProcess remoteProcess) {
        super("Disconnect",
              "Disconnect the log view from remote Spark job",
              Optional.ofNullable(StreamUtil.getImageResourceFile("/icons/SparkJobDisconnect.png"))
                      .map(Icon.class::cast)
                      .orElse(AllIcons.Actions.Exit));

        this.remoteProcess = remoteProcess;
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        // Disconnect Spark Job log receiving
        getSparkRemoteProcess().ifPresent(SparkBatchJobRemoteProcess::disconnect);
    }

    public Optional<SparkBatchJobRemoteProcess> getSparkRemoteProcess() {
        return Optional.ofNullable(remoteProcess);
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();

        presentation.setEnabled(isEnabled);
    }
}
