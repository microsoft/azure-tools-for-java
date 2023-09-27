/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.IconLoader;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;

import javax.swing.*;
import java.awt.*;

public class ContainerRegistryTypeComboBox extends ComboBox<String> {

    public static final String ACR = "Azure Container Registries";
    public static final String DOCKER_HUB = "Docker Hub Registry";
    public static final String OTHER = "Other public registry";

    public ContainerRegistryTypeComboBox() {
        super();
        this.setModel(new DefaultComboBoxModel<>(new String[]{ACR, DOCKER_HUB, OTHER}));
        this.setRenderer(new MyRenderer());
    }

    private static class MyRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            this.setText(String.valueOf(value));
            final Icon icon;
            switch (String.valueOf(value)) {
                case ACR:
                    icon = IntelliJAzureIcons.getIcon(AzureIcons.ContainerRegistry.MODULE);
                    break;
                case DOCKER_HUB:
                    icon = IconLoader.getIcon("/icons/Docker.svg", ContainerRegistryTypeComboBox.class);
                    break;
                default:
                    icon = IconLoader.getIcon("/icons/Registry.svg", ContainerRegistryTypeComboBox.class);
            }
            this.setIcon(icon);
            return this;
        }
    }
}
