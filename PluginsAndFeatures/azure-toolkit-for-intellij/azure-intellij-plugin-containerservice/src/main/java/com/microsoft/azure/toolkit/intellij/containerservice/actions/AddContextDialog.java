/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerservice.actions;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerservice.AzureContainerService;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesCluster;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AddContextDialog extends AzureDialog<KubernetesCluster> implements AzureFormPanel<KubernetesCluster> {
    private JLabel lblName;
    private AzureComboBox<KubernetesCluster> cbCluster;
    private JLabel lblSubscription;
    private SubscriptionComboBox selectorSubscription;
    private JPanel pnlRoot;

    private final Project project;

    public AddContextDialog(@javax.annotation.Nullable Project project) {
        super(project);
        this.project = project;
        $$$setupUI$$$();
        this.init();
    }

    @Override
    protected void init() {
        super.init();
        this.cbCluster.setRequired(true);
        this.lblName.setLabelFor(cbCluster);
        this.lblSubscription.setLabelFor(selectorSubscription);
        this.selectorSubscription.addItemListener(e -> this.cbCluster.reloadItems());
    }

    @Override
    public AzureForm<KubernetesCluster> getForm() {
        return this;
    }

    @Nonnull
    @Override
    protected String getDialogTitle() {
        return "Add New Context From Azure";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public void setValue(KubernetesCluster data) {
        this.selectorSubscription.setValue(data.getSubscription());
        this.cbCluster.setValue(data);
    }

    @Nullable
    @Override
    public KubernetesCluster getValue() {
        return cbCluster.getValue();
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(selectorSubscription, cbCluster);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbCluster = new AzureComboBox<>() {
            @Nonnull
            @Override
            protected List<? extends KubernetesCluster> loadItems() {
                final Subscription subscription = AddContextDialog.this.selectorSubscription.getValue();
                return Optional.ofNullable(subscription)
                        .map(Subscription::getId)
                        .map(id -> Azure.az(AzureContainerService.class).kubernetes(id).list())
                        .orElse(Collections.emptyList());
            }

            @Override
            protected String getItemText(final Object item) {
                return item instanceof KubernetesCluster ? ((KubernetesCluster) item).getName() : super.getItemText(item);
            }

            @Nonnull
            @Override
            protected List<ExtendableTextComponent.Extension> getExtensions() {
                return Collections.emptyList();
            }
        };
    }
}
