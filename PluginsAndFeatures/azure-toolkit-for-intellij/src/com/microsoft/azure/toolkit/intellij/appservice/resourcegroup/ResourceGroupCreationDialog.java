package com.microsoft.azure.toolkit.intellij.appservice.resourcegroup;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormDialog;
import com.microsoft.azure.toolkit.lib.appservice.SimpleResourceGroup;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import javax.swing.*;

public class ResourceGroupCreationDialog extends AzureFormDialog<SimpleResourceGroup> {
    private JPanel contentPanel;

    public ResourceGroupCreationDialog(final Project project) {
        super(project);
    }

    @Override
    public AzureForm<SimpleResourceGroup> getForm() {
        return null;
    }

    @Override
    protected String getDialogTitle() {
        return "new resource group";
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }
}
