/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.component;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;

import javax.swing.ComboBoxEditor;
import javax.swing.JLabel;
import javax.swing.event.EventListenerList;
import java.awt.Component;
import java.awt.event.ActionListener;

public class FunctionAppCombineBoxEditor implements ComboBoxEditor {
    private Object item;
    private JLabel label = new JLabel();
    private EventListenerList listenerList = new EventListenerList();

    @Override
    public Component getEditorComponent() {
        return label;
    }

    @Override
    public void setItem(Object anObject) {
        item = anObject;
        if (anObject == null) {
            return;
        } else if (anObject instanceof String) {
            label.setText((String) anObject);
        } else {
            final ResourceEx<FunctionApp> function = (ResourceEx<FunctionApp>) anObject;
            label.setText(function.getResource().name());
        }
        label.getAccessibleContext().setAccessibleName(label.getText());
        label.getAccessibleContext().setAccessibleDescription(label.getText());
    }

    @Override
    public Object getItem() {
        return item;
    }

    @Override
    public void selectAll() {
        return;
    }

    @Override
    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    @Override
    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }
}
