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

package com.microsoft.azure.toolkit.intellij.appservice;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import lombok.extern.java.Log;

import javax.swing.*;

@Log
public abstract class AppConfigDialog<T extends AppServiceConfig>
    extends AzureDialog<T> {
    public static final String LABEL_ADVANCED_MODE = "More settings";
    protected Project project;
    private JCheckBox checkboxMode;
    private boolean advancedMode = false;

    public AppConfigDialog(Project project) {
        super(project);
        this.project = project;
    }

    public void setData(final T config) {
        this.getForm().setData(config);
    }

    public T getData() {
        return this.getForm().getData();
    }

    protected void toggleAdvancedMode(boolean advancedMode) {
        this.advancedMode = advancedMode;
        final AzureFormPanel<T> previousForm = advancedMode ? this.getBasicFormPanel() : this.getAdvancedFormPanel();
        final AzureFormPanel<T> followingForm = advancedMode ? this.getAdvancedFormPanel() : this.getBasicFormPanel();
        previousForm.setVisible(false);
        followingForm.setData(previousForm.getData());
        followingForm.setVisible(true);
        this.repaint();
    }

    protected void setFrontPanel(AzureFormPanel<T> panel) {
        getBasicFormPanel().setVisible(false);
        getAdvancedFormPanel().setVisible(false);
        panel.setVisible(true);
    }

    protected abstract AzureFormPanel<T> getAdvancedFormPanel();

    protected abstract AzureFormPanel<T> getBasicFormPanel();

    @Override
    protected JComponent createDoNotAskCheckbox() {
        this.checkboxMode = new JCheckBox(LABEL_ADVANCED_MODE);
        this.checkboxMode.setVisible(true);
        this.checkboxMode.setSelected(false);
        this.checkboxMode.addActionListener(e -> this.toggleAdvancedMode(this.checkboxMode.isSelected()));
        return this.checkboxMode;
    }

    public AzureForm<T> getForm() {
        return this.advancedMode ? this.getAdvancedFormPanel() : this.getBasicFormPanel();
    }
}
