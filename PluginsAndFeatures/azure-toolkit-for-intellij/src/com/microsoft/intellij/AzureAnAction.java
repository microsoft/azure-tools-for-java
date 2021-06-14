/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.azuretools.telemetrywrapper.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public abstract class AzureAnAction extends AnAction {

    public AzureAnAction() {
        super((String) null, (String) null, (Icon) null);
    }

    public AzureAnAction(Icon icon) {
        super((String) null, (String) null, icon);
    }

    public AzureAnAction(@Nullable String text) {
        super(text, (String) null, (Icon) null);
    }

    public AzureAnAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    /**
     * @param anActionEvent action event
     * @param operation operation for sending telemetry
     * @return if the action is a synchronous action, you should return true and let us complete the operation
     * if the action is an asynchronous action, you should return false and control the operation completion by yourself
     */
    public abstract boolean onActionPerformed(@NotNull AnActionEvent anActionEvent, @Nullable Operation operation);

    @Override
    public final void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        sendTelemetryOnAction(anActionEvent, "Execute", null);
        final String serviceName = transformHDInsight(getServiceName(anActionEvent), anActionEvent);
        final String operationName = getOperationName(anActionEvent);

        final Operation operation = TelemetryManager.createOperation(serviceName, operationName);
        boolean actionReturnVal = true;
        try {
            operation.start();
            EventUtil.logEvent(EventType.info, operation, buildProp(anActionEvent, null));
            actionReturnVal = onActionPerformed(anActionEvent, operation);
        } catch (final RuntimeException ex) {
            EventUtil.logError(operation, ErrorType.systemError, ex, null, null);
            final IntellijAzureMessage message = new IntellijAzureMessage(IAzureMessage.Type.ERROR, "failed to " + operationName);
            message.setProject(getEventProject(anActionEvent));
            message.setBackgrounded(false);
            message.setPayload(ex);
            AzureMessager.getMessager().show(message);
        } finally {
            if (actionReturnVal) {
                operation.complete();
            }
        }
    }

    public void sendTelemetryOnAction(AnActionEvent anActionEvent, final String action, Map<String, String> extraInfo) {
        AppInsightsClient.createByType(AppInsightsClient.EventType.Action, anActionEvent.getPresentation().getText(),
            null, buildProp(anActionEvent, extraInfo));
    }

    private Map<String, String> buildProp(AnActionEvent anActionEvent, Map<String, String> extraInfo) {
        final Map<String, String> properties = new HashMap<>();
        properties.put("Text", anActionEvent.getPresentation().getText());
        properties.put("Description", anActionEvent.getPresentation().getDescription());
        properties.put("Place", anActionEvent.getPlace());
        properties.put("ActionId", anActionEvent.getActionManager().getId(this));
        if (extraInfo != null) {
            properties.putAll(extraInfo);
        }
        if (this instanceof TelemetryProperties) {
            properties.putAll(((TelemetryProperties) this).toProperties());
        }
        return properties;
    }

    protected String getServiceName(AnActionEvent event) {
        return TelemetryConstants.ACTION;
    }

    protected String getOperationName(AnActionEvent event) {
        try {
            return event.getPresentation().getText().toLowerCase().trim().replace(" ", "-");
        } catch (Exception ignore) {
            return "";
        }
    }

    /**
     * If eventName contains spark and hdinsight, we just think it is a spark node.
     * So set the service name to hdinsight
     * @param serviceName
     * @return
     */
    private String transformHDInsight(String serviceName, AnActionEvent event) {
        try {
            if (serviceName.equals(TelemetryConstants.ACTION)) {
                String text = event.getPresentation().getText().toLowerCase();
                if (text.contains("spark") || text.contains("hdinsight")) {
                    return TelemetryConstants.HDINSIGHT;
                }
                String place = event.getPlace().toLowerCase();
                if (place.contains("spark") || place.contains("hdinsight")) {
                    return TelemetryConstants.HDINSIGHT;
                }
            }
            return serviceName;
        } catch (Exception ignore) {
        }
        return serviceName;
    }

    public void sendTelemetryOnSuccess(AnActionEvent anActionEvent, Map<String, String> extraInfo) {
        sendTelemetryOnAction(anActionEvent, "Success", extraInfo);
    }

    public void sendTelemetryOnException(AnActionEvent anActionEvent, Throwable e) {
        Map<String, String> extraInfo = new HashMap<>();
        extraInfo.put("ErrorMessage", e.getMessage());
        this.sendTelemetryOnAction(anActionEvent, "Exception", extraInfo);
    }
}
