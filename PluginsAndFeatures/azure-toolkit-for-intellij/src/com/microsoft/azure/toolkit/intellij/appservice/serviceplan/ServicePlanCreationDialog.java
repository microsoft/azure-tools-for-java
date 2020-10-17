package com.microsoft.azure.toolkit.intellij.appservice.serviceplan;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormDialog;
import com.microsoft.azure.toolkit.lib.appservice.SimpleServicePlan;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import javax.swing.*;

public class ServicePlanCreationDialog extends AzureFormDialog<SimpleServicePlan> {
    private JPanel contentPanel;

    public ServicePlanCreationDialog(final Project project) {
        super(project);
    }

    @Override
    public AzureForm<SimpleServicePlan> getForm() {
        return null;
    }

    @Override
    protected String getDialogTitle() {
        return "new service plan";
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }
}
