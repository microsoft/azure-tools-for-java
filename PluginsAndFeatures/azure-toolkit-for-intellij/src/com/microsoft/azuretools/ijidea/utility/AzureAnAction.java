/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.ijidea.utility;

import com.intellij.openapi.actionSystem.*;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
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

    public abstract void onActionPerformed(AnActionEvent anActionEvent);

    @Override
    public final void actionPerformed(AnActionEvent anActionEvent) {
        sendTelemetryOnAction(anActionEvent, "Execute", null);
        onActionPerformed(anActionEvent);
    }

    public void sendTelemetryOnAction(AnActionEvent anActionEvent, final String action, Map<String, String> extraInfo) {
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
        AppInsightsClient.createByType(AppInsightsClient.EventType.Action, anActionEvent.getPresentation().getText(), null, properties);
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
