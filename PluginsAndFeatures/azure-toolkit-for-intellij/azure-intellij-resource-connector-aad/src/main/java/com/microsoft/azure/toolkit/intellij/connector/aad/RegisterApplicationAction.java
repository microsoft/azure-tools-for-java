/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.graph.models.Application;
import com.microsoft.graph.models.ImplicitGrantSettings;
import com.microsoft.graph.models.WebApplication;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.microsoft.azure.toolkit.lib.Azure.az;

/**
 * Displays UI to create a new Azure AD application.
 * <p>
 * ComponentNotRegistered is suppressed, because IntelliJ isn't finding the reference in resources/META-INF.
 */
@SuppressWarnings("ComponentNotRegistered")
public class RegisterApplicationAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(RegisterApplicationAction.class);

    public RegisterApplicationAction() {
        super(MessageBundle.message("action.AzureToolkit.AD.AzureRegisterApp.text"));
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        try {
            // throws an exception if user is not signed in
            az(AzureAccount.class).account();
        } catch (AzureToolkitAuthenticationException ex) {
            LOG.debug("user is not signed in", ex);
            e.getPresentation().setEnabled(false);
        }
    }

    @Override
    @ExceptionNotification
    @AzureOperation(name = "aad.register_application", type = AzureOperation.Type.ACTION)
    public void actionPerformed(@Nonnull AnActionEvent e) {
        var project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        // Show dialog to enter the application data, then create in background after user confirmed
        var dialog = new RegisterApplicationInAzureAdDialog(project);
        if (dialog.showAndGet()) {
            var subscription = dialog.getSubscription();
            if (subscription == null) {
                return;
            }

            var task = new RegisterApplicationTask(project, dialog.getForm().getValue(), subscription);
            var title = MessageBundle.message("action.azure.aad.registerApp.registeringApplication");

            AzureTaskManager.getInstance().runInBackground(title, task);
        }
    }

    /**
     * Task, which creates a new Azure AD application based on the data entered into the form.
     */
    @RequiredArgsConstructor
    static class RegisterApplicationTask implements Runnable {
        private final Project project;
        @Nonnull
        private final ApplicationRegistrationModel model;
        @Nonnull
        private final Subscription subscription;

        @Override
        @AzureOperation(name = "aad.create_application", type = AzureOperation.Type.TASK)
        public void run() {
            var validSuffix = new StringBuilder();
            for (char c : (model.getDisplayName() + UUID.randomUUID().toString().substring(0, 6)).toCharArray()) {
                if (Character.isLetterOrDigit(c)) {
                    validSuffix.append(c);
                }
            }

            var params = new Application();
            params.displayName = model.getDisplayName();
            params.identifierUris = Collections.singletonList("https://" + model.getDomain() + "/" + validSuffix);
            params.web = new WebApplication();
            params.web.redirectUris = List.copyOf(model.getCallbackUrls());
            if (model.isMultiTenant()) {
                params.signInAudience = "AzureADMultipleOrgs";
            } else {
                params.signInAudience = "AzureADMyOrg";
            }
            // Grant "ID Tokens" permissions for the web application
            params.web.implicitGrantSettings = new ImplicitGrantSettings();
            params.web.implicitGrantSettings.enableIdTokenIssuance = true;

            // read-only access if the client ID was defined by the user
            var clientID = model.getClientId();
            if (StringUtil.isNotEmpty(clientID)) {
                params.appId = model.getClientId();
                showApplicationTemplateDialog(subscription, params);
                return;
            }

            var graphClient = AzureUtils.createGraphClient(subscription);
            var application = graphClient.applications().buildRequest().post(params);
            assert application.id != null;

            var newCredentials = AzureUtils.createApplicationClientSecret(graphClient, application);
            if (application.passwordCredentials == null) {
                application.passwordCredentials = Collections.singletonList(newCredentials);
            } else {
                application.passwordCredentials.add(newCredentials);
            }

            // now display the new application in the "Application templates dialog"
            showApplicationTemplateDialog(subscription, application);
        }

        @AzureOperation(name = "aad.show_application_template", type = AzureOperation.Type.TASK)
        private void showApplicationTemplateDialog(@Nonnull Subscription subscription, @Nonnull Application application) {
            AzureTaskManager.getInstance().runLater(() -> {
                new AzureApplicationTemplateDialog(project, new SubscriptionApplicationPair(subscription, application)).show();
            });
        }
    }
}
