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
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobDisconnectEvent;
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobRemoteProcess;
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobSubmittedEvent;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// We didn't extend SparkBatchJobDisconnectAction from AzureAction to avoid introducing a new operation object. We think
// SparkBatchJobDisconnectAction must belong to one remote run or remote debug action. Therefore, we should pass the
// operation object in that action to this class rather than create a new one in AzureAnAction class
public class SparkBatchJobDisconnectAction extends AnAction {
    @Nullable
    private SparkBatchJobRemoteProcess remoteProcess;
    private boolean isEnabled = false;
    @Nullable
    private Operation operation = null;

    public SparkBatchJobDisconnectAction() {
        super();
    }

    public SparkBatchJobDisconnectAction(@Nullable SparkBatchJobRemoteProcess remoteProcess, @Nullable Operation operation) {
        super("Disconnect",
              "Disconnect the log view from remote Spark job",
              Optional.ofNullable(StreamUtil.getImageResourceFile("/icons/SparkJobDisconnect.png"))
                      .map(Icon.class::cast)
                      .orElse(AllIcons.Actions.Exit));

        this.remoteProcess = remoteProcess;
        this.operation = operation;

        // Listen Spark Job submitted event to enable the disconnect button
        remoteProcess.getEventSubject()
                .filter(SparkBatchJobSubmittedEvent.class::isInstance)
                .subscribe(job -> setEnabled(true));
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        remoteProcess.getEventSubject().onNext(new SparkBatchJobDisconnectEvent());

        Map<String, String> properties = new HashMap<>();
        properties.put("Text", anActionEvent.getPresentation().getText());
        properties.put("Description", anActionEvent.getPresentation().getDescription());
        properties.put("Place", anActionEvent.getPlace());
        properties.put("ActionId", anActionEvent.getActionManager().getId(this));

        properties.put("isDisconnectButtonClicked", "true");
        EventUtil.logEvent(EventType.info, this.operation, properties);

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
