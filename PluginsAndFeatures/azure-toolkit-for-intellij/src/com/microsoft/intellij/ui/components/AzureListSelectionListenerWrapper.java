/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui.components;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.AppInsightsClient;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.HashMap;
import java.util.Map;

public abstract class AzureListSelectionListenerWrapper implements ListSelectionListener {
    private static final String COMPOSITE = "Composite";
    private static final String WIDGEETNAME = "WidgetName";
    private static final String WIDGETTYPE = "Type";
    private static final String SELECTEVENT = "ListValueChanged";

    private String compositeName;
    private String widgetName;
    private Map<String, String> properties;

    /**
     *
     * @param cpName String, The name of the composite hold the listener source.
     * @param wgtName String, The name of the listener source.
     * @param props Map, Properties need to be sent.
     */
    public AzureListSelectionListenerWrapper(@NotNull String cpName,
                                             @NotNull String wgtName, @Nullable Map<String, String> props) {
        this.compositeName = cpName;
        this.widgetName = wgtName;
        this.properties = props;
    }

    @Override
    public final void valueChanged(ListSelectionEvent e) {
        sendTelemetry(e);
        valueChangedFunc(e);
    }

    protected abstract void valueChangedFunc(ListSelectionEvent event);

    private void sendTelemetry(ListSelectionEvent event) {
        Map<String, String> telemetryProperties = new HashMap<String, String>();
        telemetryProperties.put(COMPOSITE, compositeName);
        telemetryProperties.put(WIDGEETNAME, widgetName);
        Object source = event.getSource();
        if (source != null) {
            String type = event.getSource().getClass().getName();
            telemetryProperties.put(WIDGETTYPE, type);
        }

        if (null != properties) {
            telemetryProperties.putAll(properties);
        }

        String eventName = String.format("%s.%s.%s", compositeName, widgetName, SELECTEVENT);
        AppInsightsClient.create(eventName, null, telemetryProperties, false);
    }
}
