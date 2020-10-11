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

package com.microsoft.azure.appservice.webapp.component;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.appservice.component.form.AzureForm;
import com.microsoft.azure.appservice.component.form.AzureFormPanel;
import com.microsoft.azure.appservice.webapp.WebAppConfig;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class WebAppCreationDialog extends AzureDialogWrapper
        implements AzureForm<WebAppConfig> {
    public static final String LABEL_ADVANCED_MODE = "More settings";
    protected Project project;
    private JCheckBox checkboxMode;
    private boolean advancedMode = false;
    public static final String TITLE_CREATE_WEBAPP_DIALOG = "Create Web App";
    private JPanel panel;
    private WebAppConfigFormPanelAdvanced advancedForm;
    private WebAppConfigFormPanelBasic basicForm;

    public WebAppCreationDialog(Project project) {
        super(project, true);
        this.project = project;
        setTitle(this.getDialogTitle());
        setModal(true);
        this.init();
    }

    protected void toggleAdvancedMode(boolean advancedMode) {
        this.advancedMode = advancedMode;
        if (advancedMode) {
            basicForm.getContentPanel().setVisible(false);
            basicForm.setVisible(false);
            advancedForm.getContentPanel().setVisible(true);
            advancedForm.setVisible(true);
        } else {
            basicForm.getContentPanel().setVisible(true);
            basicForm.setVisible(true);
            advancedForm.getContentPanel().setVisible(false);
            advancedForm.setVisible(false);
        }
        this.pack();
        this.repaint();
    }

    @Override
    protected void init() {
        super.init();
        this.toggleAdvancedMode(false);
    }

    @Override
    protected JComponent createDoNotAskCheckbox() {
        this.checkboxMode = new JCheckBox(LABEL_ADVANCED_MODE);
        this.checkboxMode.setVisible(true);
        this.checkboxMode.setSelected(false);
        this.checkboxMode.addActionListener(e -> this.toggleAdvancedMode(this.checkboxMode.isSelected()));
        return this.checkboxMode;
    }

    @Override
    public WebAppConfig getData() {
        return null;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.panel;
    }

    protected String getDialogTitle() {
        return TITLE_CREATE_WEBAPP_DIALOG;
    }
}
