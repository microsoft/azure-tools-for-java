/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.integration.services.connectionstring;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.IconLoader;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;

import javax.swing.*;
import java.awt.*;

public class ConnectionStringTypeComboBox extends ComboBox<String> {

    public static final String STORAGE = "Microsoft.Storage";
//    public static final String COSMOS_MONGO = "Microsoft.DocumentDB/mongodbDatabases";

    public ConnectionStringTypeComboBox() {
        super();
        this.setModel(new DefaultComboBoxModel<>(new String[]{STORAGE}));
        this.setRenderer(new MyRenderer());
    }

    private static class MyRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            final String type = String.valueOf(value);
            final String text = "Azure Storage account";
            this.setText(text);
            final Icon icon = IntelliJAzureIcons.getIcon(AzureIcons.StorageAccount.MODULE);
            this.setIcon(icon);
            return this;
        }
    }
}
