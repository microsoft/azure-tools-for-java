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

import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;

public class FunctionAppCombineBoxRender extends ListCellRendererWrapper {
    private final JComboBox comboBox;
    private final int cellHeight;
    private static final String TEMPLATE_STRING = "<html><div>TEMPLATE</div><small>TEMPLATE</small></html>";

    public FunctionAppCombineBoxRender(JComboBox comboBox) {
        this.comboBox = comboBox;
        final JLabel template = new JLabel(TEMPLATE_STRING);
        this.cellHeight = template.getPreferredSize().height;
    }

    @Override
    public void customize(JList list, Object value, int index, boolean b, boolean b1) {
        if (value == null) {
            return;
        } else if (value instanceof String) {
            setText(getStringLabelText((String) value));
        } else {
            final ResourceEx<FunctionApp> function = (ResourceEx<FunctionApp>) value;
            // For label in combobox textfield, just show function app name
            final String text = index >= 0 ? getFunctionAppLabelText(function.getResource()) : function.getResource().name();
            setText(text);
        }
        list.setFixedCellHeight(cellHeight);
    }

    private String getStringLabelText(String message) {
        return comboBox.isPopupVisible() ?
                String.format("<html><div>%s</div><small></small></html>", message) : message;
    }

    private String getFunctionAppLabelText(FunctionApp functionApp) {
        final String name = functionApp.name();
        final String os = StringUtils.capitalize(functionApp.operatingSystem().toString());
        final String resourceGroup = functionApp.resourceGroupName();

        return comboBox.isPopupVisible() ? String.format("<html><div>%s</div></div><small>OS:%s " +
                "ResourceGroup:%s</small></html>", name, os, resourceGroup) : name;
    }
}
