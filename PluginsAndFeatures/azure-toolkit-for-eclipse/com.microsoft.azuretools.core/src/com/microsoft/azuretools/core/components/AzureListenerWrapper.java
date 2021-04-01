/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.components;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.AppInsightsClient;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public abstract class AzureListenerWrapper implements Listener {
    private static final String COMPOSITE = "Composite";
    private static final String WIDGETNAME = "WidgetName";
    private static final String WIDGET = "Widget";
    private static final String SWTEVENT = "SWTEventType";

    private String compositeName;
    private String widgetName;
    private Map<String, String> properties;

    /**
     * @param cpName.
     * @param wgtName.
     * @param prop.
     */
    public AzureListenerWrapper(@NotNull String cpName, @NotNull String wgtName, @Nullable Map<String, String> props) {
        this.compositeName = cpName;
        this.widgetName = wgtName;
        this.properties = props;
    }

    @Override
    public final void handleEvent(Event event) {
        sendTelemetry(event);
        handleEventFunc(event);
    }

    protected abstract void handleEventFunc(Event event);

    private void sendTelemetry(Event event) {
        if (event == null) {
            return;
        }

        String widget = event.widget != null ? event.widget.toString() : null;

        Map<String, String> telemetryProperties = new HashMap<String, String>();
        telemetryProperties.put(COMPOSITE, compositeName);
        telemetryProperties.put(WIDGETNAME, widgetName);
        if (null != widget) {
            telemetryProperties.put(WIDGET, widget);
        }
        telemetryProperties.put(SWTEVENT, String.valueOf(event.type));
        if (null != properties) {
            telemetryProperties.putAll(properties);
        }
        String eventName = String.format("%s.%s.%d", compositeName, widgetName, event.type);
        AppInsightsClient.create(eventName, null, telemetryProperties, false);
    }
}
