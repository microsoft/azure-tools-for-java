/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.graph.models.Application;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;

/**
 * Dialog which displays code templates for a list of Azure AD applications.
 */
class AzureApplicationTemplateDialog extends AzureDialog<Application> {
    @NotNull
    private final ApplicationTemplateForm form;

    AzureApplicationTemplateDialog(@NotNull Project project,
                                   @NotNull GraphServiceClient<Request> graphClient,
                                   @NotNull Subscription subscription) {
        this(project, graphClient, null, subscription);
    }

    AzureApplicationTemplateDialog(@NotNull Project project,
                                   @NotNull Application application,
                                   @NotNull Subscription subscription) {
        this(project, null, application, subscription);
    }

    private AzureApplicationTemplateDialog(@NotNull Project project,
                                           @Nullable GraphServiceClient<Request> graphClient,
                                           @Nullable Application application,
                                           @NotNull Subscription subscription) {
        super(project);
        assert graphClient != null || application != null;

        var predefinedItems = application == null ? null : Collections.singletonList(application);
        form = new ApplicationTemplateForm(project, subscription, graphClient, predefinedItems);
        init();
    }

    @Override
    public AzureForm<Application> getForm() {
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
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction()};
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return form.getPreferredFocusedComponent();
    }
}
