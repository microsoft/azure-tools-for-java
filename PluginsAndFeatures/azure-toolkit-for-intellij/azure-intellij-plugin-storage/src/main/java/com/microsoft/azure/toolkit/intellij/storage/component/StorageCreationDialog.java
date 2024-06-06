/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.component;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.storage.IStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class StorageCreationDialog extends AzureDialog<String>
    implements AzureForm<String> {
    private final AbstractAzResourceModule<?, ? extends IStorageAccount, ?> module;
    private JBLabel labelDescription;
    private JPanel contentPanel;
    private StorageNameTextField textName;

    public StorageCreationDialog(AbstractAzResourceModule<?, ? extends IStorageAccount, ?> module, Project project) {
        super(project);
        this.setTitle("New " + module.getResourceTypeName());
        this.module = module;
        this.init();
        this.textName.setModule(module);
        this.pack();
    }

    protected String getDialogTitle() {
        return "New Storage";
    }

    @Override
    public AzureForm<String> getForm() {
        return this;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }

    @Override
    public String getValue() {
        return this.textName.getValue();
    }

    @Override
    public void setValue(final String name) {
        this.textName.setValue(name);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.textName);
    }
}
