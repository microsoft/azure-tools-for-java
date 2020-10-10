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

package com.microsoft.intellij.runner.appservice.webapp;

import com.intellij.openapi.project.Project;
import com.microsoft.intellij.runner.appservice.AppServiceAdvancedConfigPanel;
import com.microsoft.intellij.runner.appservice.AppServiceBasicConfigPanel;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class WebAppCreationDialog extends AzureDialogWrapper {
    public static final String LABEL_ADVANCED_MODE = "More settings";
    public static final String TITLE_CREATE_WEBAPP_DIALOG = "Create Web App";
    private Project project;
    private JPanel panel;
    private AppServiceAdvancedConfigPanel advancedPanel;
    private AppServiceBasicConfigPanel basicPanel;
    private JCheckBox checkboxMode;

    public WebAppCreationDialog(Project project) {
        super(project, true);

        this.project = project;
        setTitle(TITLE_CREATE_WEBAPP_DIALOG);
        setModal(true);
        this.init();
    }

    protected void toggleAdvancedMode(boolean advancedMode) {
        final Dimension size = this.getSize();
        if (advancedMode) {
            this.basicPanel.setVisible(false);
            this.basicPanel.getContent().setVisible(false);
            this.advancedPanel.setVisible(true);
            this.advancedPanel.getContent().setVisible(true);
        } else {
            this.advancedPanel.setVisible(false);
            this.advancedPanel.getContent().setVisible(false);
            this.basicPanel.setVisible(true);
            this.basicPanel.getContent().setVisible(true);
        }
        this.pack();
        this.repaint();
    }

    @Override
    protected JComponent createDoNotAskCheckbox() {
        this.checkboxMode = new JCheckBox(LABEL_ADVANCED_MODE);
        this.checkboxMode.setVisible(true);
        this.checkboxMode.setSelected(false);
        this.checkboxMode.addActionListener(e -> this.toggleAdvancedMode(this.checkboxMode.isSelected()));
        return this.checkboxMode;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.panel;
    }

    @Override
    protected void init() {
        super.init();
        this.toggleAdvancedMode(false);
    }
}
