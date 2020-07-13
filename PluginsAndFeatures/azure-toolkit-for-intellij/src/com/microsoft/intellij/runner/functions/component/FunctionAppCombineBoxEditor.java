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

package com.microsoft.intellij.runner.functions.component;

import com.azure.resourcemanager.appservice.models.FunctionApp;
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
