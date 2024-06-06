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
    private final List<JComponent> contentComponentList = new ArrayList<>();

    public AzureHideableTitledSeparator() {
        this.updateIcon(isExpanded);
        this.addMouseListener(new MouseAdapter() {
            public void mouseReleased(@NotNull MouseEvent e) {
                final boolean enabled = isEnabled();
                if (enabled) {
                    AzureHideableTitledSeparator.this.update(!AzureHideableTitledSeparator.this.isExpanded);
                }
            }
        });
    }

    public void expand() {
        this.update(true);
    }

    public void collapse() {
        this.update(false);
    }

    public void toggle(boolean expand) {
        this.update(expand);
    }

    public void addContentComponent(JComponent component) {
        contentComponentList.add(component);
        component.setVisible(isExpanded);
    }

    private void update(boolean expand) {
        this.isExpanded = expand;
        for (final JComponent component : contentComponentList) {
            component.setVisible(expand);
        }
        this.updateIcon(expand);
    }

    private void updateIcon(boolean expand) {
        final Icon icon = expand ? AllIcons.General.ArrowDown : AllIcons.General.ArrowRight;
        final JLabel label = this.getLabel();
        label.setIcon(icon);
    }
}
