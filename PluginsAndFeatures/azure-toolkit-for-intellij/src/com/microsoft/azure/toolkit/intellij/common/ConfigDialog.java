/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.extern.java.Log;

import javax.swing.*;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

@Log
public abstract class ConfigDialog<T> extends AzureDialog<T> {
    protected Project project;
    private JCheckBox checkboxMode;
    private boolean advancedMode = false;

    public ConfigDialog(Project project) {
        super(project);
        this.project = project;
    }

    public void setData(final T config) {
        this.getForm().setData(config);
    }

    public T getData() {
        return this.getForm().getData();
    }


    @AzureOperation(name = "common.toggle_config_mode.ui", type = AzureOperation.Type.TASK)
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
        this.checkboxMode = new JCheckBox(message("appService.appConfig.advancedMode"));
        this.checkboxMode.setVisible(true);
        this.checkboxMode.setSelected(false);
        this.checkboxMode.addActionListener(e -> this.toggleAdvancedMode(this.checkboxMode.isSelected()));
        return this.checkboxMode;
    }

    public AzureForm<T> getForm() {
        return this.advancedMode ? this.getAdvancedFormPanel() : this.getBasicFormPanel();
    }
}
