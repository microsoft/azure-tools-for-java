/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.component;

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
