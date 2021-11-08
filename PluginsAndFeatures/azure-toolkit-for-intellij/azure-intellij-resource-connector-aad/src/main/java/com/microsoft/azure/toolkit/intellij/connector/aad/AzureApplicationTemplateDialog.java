/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.Application;
import com.microsoft.graph.models.PasswordCredential;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

/**
 * Dialog which displays code templates for a list of Azure AD applications.
 * <p>
 * There are two modes:
 * 1. A predefined application is displayed
 * 2. The user can choose subscription and application to update the templates.
 */
class AzureApplicationTemplateDialog extends AzureDialog<Application> {
    @Nonnull
    private final ApplicationTemplateForm form;
    @Nonnull
    private final Project project;
    @Nullable
    private final SubscriptionApplicationPair predefinedData;
    private final NewClientSecretAction clientSecretAction = new NewClientSecretAction();

    /**
     * @param project        The current project
     * @param predefinedData If only predefined data should be shown, then this is the matching pair of subscription and application.
     */
    AzureApplicationTemplateDialog(@Nonnull Project project, @Nullable SubscriptionApplicationPair predefinedData) {
        super(project);
        this.project = project;
        this.predefinedData = predefinedData;

        form = new ApplicationTemplateForm(project, predefinedData == null ? null : predefinedData.getApplication());
        init();

        // the predefined application may have no id if the user manually provided the appId
        clientSecretAction.setEnabled(predefinedData == null || predefinedData.getApplication().id != null);
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
    @Nonnull
    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }

    @Override
    @Nonnull
    protected Action[] createLeftSideActions() {
        return new Action[]{clientSecretAction, new CopyEditorContentAction()};
    }

    @Override
    @Nullable
    public JComponent getPreferredFocusedComponent() {
        return form.getPreferredFocusedComponent();
    }

    private class CopyEditorContentAction extends AbstractAction {
        public CopyEditorContentAction() {
            super(MessageBundle.message("templateDialog.applications.copyEditorContent"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            var editor = form.getCurrentEditor().getEditor();
            if (editor != null) {
                CopyPasteManager.getInstance().setContents(new StringSelection(editor.getDocument().getText()));
            }
        }
    }

    private class NewClientSecretAction extends AbstractAction {
        public NewClientSecretAction() {
            super(MessageBundle.message("templateDialog.applications.createClientSecret"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            AzureTaskManager.getInstance().runInModal(MessageBundle.message("templateDialog.applications.creatingClientSecret"), false, () -> {
                try {
                    // prefer the pair of predefined data, if available.
                    // In this case the form isn't necessarily referencing the same subscription as its UI box hidden
                    var application = predefinedData != null ? predefinedData.getApplication() : form.getValue();
                    var subscription = predefinedData != null ? predefinedData.getSubscription() : form.getSubscription();
                    if (application == null || subscription == null) {
                        return;
                    }

                    var graphClient = AzureUtils.createGraphClient(subscription);
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
}
