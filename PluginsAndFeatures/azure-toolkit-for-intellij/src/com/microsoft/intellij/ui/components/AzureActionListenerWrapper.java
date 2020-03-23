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

package com.microsoft.intellij.ui.components;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.AppInsightsClient;

import javax.swing.JComboBox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public abstract class AzureActionListenerWrapper implements ActionListener {
    private static final String COMPOSITE = "Composite";
    private static final String WIDGEETNAME = "WidgetName";
    private static final String CMD = "Command";
    private static final String WIDGETTYPE = "Type";
    private static final String COMBOSELECTED = "SelectedComboItem";

    private String compositeName;
    private String widgetName;
    private Map<String, String> properties;

    /**
     *
     * @param cpName String, The name of the composite hold the listener source.
     * @param wgtName String, The name of the listener source.
     * @param props Map, Properties need to be sent.
     */
    public AzureActionListenerWrapper(@NotNull String cpName,
                                      @NotNull String wgtName, @Nullable Map<String, String> props) {
        this.compositeName = cpName;
        this.widgetName = wgtName;
        this.properties = props;
    }

    @Override
    public final void actionPerformed(ActionEvent e) {
        sendTelemetry(e);
        actionPerformedFunc(e);
    }

    protected abstract void actionPerformedFunc(ActionEvent e);

    private void sendTelemetry(ActionEvent event) {
        Map<String, String> telemetryProperties = new HashMap<String, String>();
        String cmd = event.getActionCommand();
        telemetryProperties.put(CMD, (cmd != null ? cmd : ""));
        telemetryProperties.put(WIDGEETNAME, this.widgetName);
        telemetryProperties.put(COMPOSITE, this.compositeName);

        Object source = event.getSource();
        if (source != null) {
            String type = event.getSource().getClass().getName();
            telemetryProperties.put(WIDGETTYPE, type);

            if (source instanceof JComboBox) {
                JComboBox cb = (JComboBox)source;
                Object selectedItem = cb.getSelectedItem();
                if (selectedItem !=  null) {
                    telemetryProperties.put(COMBOSELECTED, selectedItem.toString());
                }
            }
        }

        if (null != properties) {
            telemetryProperties.putAll(properties);
        }

        String eventName = String.format("%s.%s.%s", compositeName, widgetName, cmd);
        AppInsightsClient.create(eventName, null, telemetryProperties, false);
    }
}
