/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.azure.resourcemanager.authorization.fluent.GraphRbacManagementClient;
import com.azure.resourcemanager.authorization.fluent.models.ApplicationInner;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;

/**
 * Dialog which displays code templates for a list of Azure AD applications.
 */
public class AzureApplicationTemplateDialog extends AzureDialog<ApplicationInner> {
    @NotNull
    private final ApplicationTemplateForm form;

    public AzureApplicationTemplateDialog(@NotNull Project project,
                                          @NotNull GraphRbacManagementClient graphClient,
                                          @NotNull Subscription subscription) {
        this(project, graphClient, null, subscription);
    }

    public AzureApplicationTemplateDialog(@NotNull Project project,
                                          @NotNull ApplicationInner application,
                                          @NotNull Subscription subscription) {
        this(project, null, application, subscription);
    }

    private AzureApplicationTemplateDialog(@NotNull Project project,
                                           @Nullable GraphRbacManagementClient graphClient,
                                           @Nullable ApplicationInner application,
                                           @NotNull Subscription subscription) {
        super(project);
        assert graphClient != null || application != null;

        var predefinedItems = application == null ? null : Collections.singletonList(application);
        form = new ApplicationTemplateForm(project, subscription, graphClient, predefinedItems);
        form.init();

        init();
    }

    @Override
    public AzureForm<ApplicationInner> getForm() {
        return form;
    }

    @Override
    protected String getDialogTitle() {
        return MessageBundle.message("templateDialog.title");
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        return form.getContentPanel();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return form.getPreferredFocusedComponent();
    }
}
