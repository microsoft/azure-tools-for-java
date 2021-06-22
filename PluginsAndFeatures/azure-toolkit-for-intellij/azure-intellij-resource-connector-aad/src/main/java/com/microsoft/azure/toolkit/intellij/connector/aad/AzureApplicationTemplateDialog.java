/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.Application;
import com.microsoft.graph.models.PasswordCredential;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Dialog which displays code templates for a list of Azure AD applications.
 */
class AzureApplicationTemplateDialog extends AzureDialog<Application> {
    @NotNull
    private final ApplicationTemplateForm form;
    @NotNull
    private final Project project;
    @NotNull
    private final GraphServiceClient<Request> graphClient;

    AzureApplicationTemplateDialog(@NotNull Project project,
                                   @NotNull GraphServiceClient<Request> graphClient,
                                   @NotNull Subscription subscription,
                                   @Nullable Application application) {
        super(project);
        this.project = project;
        this.graphClient = graphClient;

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
    protected Action @NotNull [] createLeftSideActions() {
        return new Action[]{
                new AbstractAction(MessageBundle.message("templateDialog.applications.createClientSecret")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        AzureTaskManager.getInstance().runInModal(MessageBundle.message("templateDialog.applications.creatingClientSecret"), false, () -> {
                            try {
                                var application = form.getData();
                                var secret = AzureUtils.createApplicationClientSecret(graphClient, application);

                                // update UI with the updated application
                                AzureTaskManager.getInstance().runLater(() -> {
                                    // the new credentials must be first
                                    var credentials = new ArrayList<PasswordCredential>();
                                    credentials.add(secret);
                                    credentials.addAll(application.passwordCredentials);

                                    // refresh application in-place
                                    application.passwordCredentials = credentials;

                                    form.refreshSelectedApplication();
                                });
                            } catch (ClientException ex) {
                                // not using AzureMessanger, because it's unable to display an error message
                                // on top of the application templates dialog.
                                String details = null;
                                if (ex instanceof GraphServiceException) {
                                    var error = ((GraphServiceException) ex).getServiceError();
                                    if (error != null) {
                                        details = error.message;
                                    }
                                }

                                var message = details == null
                                        ? MessageBundle.message("templateDialog.createCredentialsError.textUnknown")
                                        : MessageBundle.message("templateDialog.createCredentialsError.text", details);

                                AzureTaskManager.getInstance().runLater(() -> {
                                    Messages.showErrorDialog(project,
                                            message,
                                            MessageBundle.message("templateDialog.createCredentialsError.title"));
                                });
                            }
                        });
                    }
                }
        };
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return form.getPreferredFocusedComponent();
    }
}
