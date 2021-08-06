/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.helpers.storage;

import com.intellij.openapi.fileEditor.FileEditor;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class FileEditorVirtualNode<T extends FileEditor> extends Node implements TelemetryProperties {
    private T fileEditor;

    public FileEditorVirtualNode(final T t, final String name) {
        super(t.getClass().getSimpleName(), name);
        this.fileEditor = t;
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        if (fileEditor instanceof TelemetryProperties) {
            properties.putAll(((TelemetryProperties) fileEditor).toProperties());
        }
        return properties;
    }

    public JMenuItem createJMenuItem(final String actionName) {
        final JMenuItem menuItem = new JMenuItem(actionName);
        final NodeAction nodeAction = getNodeActionByName(actionName);
        if (nodeAction != null) {
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    nodeAction.fireNodeActionEvent();
                }
            });
        }
        return menuItem;
    }
}
