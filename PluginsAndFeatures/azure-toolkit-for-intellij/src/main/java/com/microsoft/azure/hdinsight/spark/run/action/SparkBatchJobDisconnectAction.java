/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.run.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobDisconnectEvent;
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobRemoteProcess;
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobSubmittedEvent;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import org.jetbrains.annotations.Nullable;

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

    public void init(@Nullable SparkBatchJobRemoteProcess remoteProcess, @Nullable Operation operation) {
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
