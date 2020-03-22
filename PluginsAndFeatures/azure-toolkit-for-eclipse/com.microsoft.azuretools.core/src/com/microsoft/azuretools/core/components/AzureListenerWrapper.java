/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
