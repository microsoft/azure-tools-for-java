/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.icons.AllIcons;
import com.intellij.ui.TitledSeparator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class AzureHideableTitledSeparator extends TitledSeparator {

    private boolean isExpanded = true;
    private List<JComponent> contentComponentList = new ArrayList<>();

    public AzureHideableTitledSeparator() {
        this.updateIcon(isExpanded);
        this.addMouseListener(new MouseAdapter() {
            public void mouseReleased(@NotNull MouseEvent e) {
                AzureHideableTitledSeparator.this.update(isEnabled() && !AzureHideableTitledSeparator.this.isExpanded);
            }
        });
    }

    public void expand() {
        this.update(true);
    }

    public void collapse() {
        this.update(false);
    }

    public void addContentComponent(JComponent component) {
        contentComponentList.add(component);
        component.setVisible(isExpanded);
    }

    private final void update(boolean expand) {
        this.isExpanded = expand;
        for (JComponent component : contentComponentList) {
            component.setVisible(expand);
        }
        this.updateIcon(expand);
    }

    private final void updateIcon(boolean expand) {
        Icon icon = expand ? AllIcons.General.ArrowDown : AllIcons.General.ArrowRight;
        JLabel label = this.getLabel();
        label.setIcon(icon);
    }

}
