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
