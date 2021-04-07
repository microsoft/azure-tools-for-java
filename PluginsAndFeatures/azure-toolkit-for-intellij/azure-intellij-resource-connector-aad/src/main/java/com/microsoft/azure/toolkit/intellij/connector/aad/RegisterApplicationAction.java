/*
 * Copyright (c) 2020 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.azure.resourcemanager.authorization.fluent.GraphRbacManagementClient;
import com.azure.resourcemanager.authorization.fluent.models.DomainInner;
import com.azure.resourcemanager.authorization.models.ApplicationCreateParameters;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.jetbrains.annotations.NotNull;
import rx.Observable;

import java.util.Collections;
import java.util.UUID;

public class RegisterApplicationAction extends AnAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance("#com.microsoft.intellij.aad");

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        assert project != null;

        // fixme this is temporary until AzureSignInAction is available to this gradle module
        if (AzureUtils.isLoggedOut()) {
            AzureMessager.getInstance().warning("Login Required", "Please sign in to create a new Azure AD application.");
            return;
        }

        doActionPerformed(project);
    }

    private void doActionPerformed(Project project) {
        var subscriptions = Azure.az(IAzureAccount.class).account().getSelectedSubscriptions();
        if (subscriptions.size() == 1) {
            // One subscription? No popup needed.
            AzureTaskManager.getInstance().runLater(() -> fetchDataAndShowDialog(project, subscriptions.get(0)));
        } else {
            // Multiple subscriptions? Popup.
            AzureTaskManager.getInstance().runLater(
                    new ChooseSubscriptionsTask(project, subscriptions, selected -> {
                        AzureTaskManager.getInstance().runLater(() -> fetchDataAndShowDialog(project, selected));
                    })
            );
        }
    }

    private static Observable<ApplicationRegistrationModel> buildRegistrationModel(@NotNull Project project,
                                                                                   @NotNull GraphRbacManagementClient client) {
        var title = MessageBundle.message("action.azure.aad.registerApp.loadDefaultDomain");
        return AzureTaskManager.getInstance().runInBackgroundAsObservable(new AzureTask<>(project, title, false, () -> {
            ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);

            var domain = client.getDomains()
                    .list()
                    .stream()
                    .filter(DomainInner::isDefault)
                    .map(DomainInner::name)
                    .findFirst()
                    .orElse("");

            var model = new ApplicationRegistrationModel();
            model.setDomain(domain);
            return model;
        }));
    }

    /**
     * Displays the dialog to enter the details for the new application.
     *
     * @param project      The IntelliJ project
     * @param subscription The selected subscription
     */
    private static void fetchDataAndShowDialog(@NotNull Project project, @NotNull Subscription subscription) {
        String tenantId = subscription.getTenantId();
        LOG.debug(String.format("Using subscription %s; tenant %s", subscription.getId(), tenantId));
        if (project.isDisposed()) {
            return;
        }

        var client = AzureUtils.createGraphClient(subscription);

        // Show dialog to enter the application data, then create in background after user confirmed
        buildRegistrationModel(project, client).subscribe(model -> {
            AzureTaskManager.getInstance().runLater(() -> {
                var dialog = new RegisterApplicationInAzureAdDialog(project, model);
                if (dialog.showAndGet()) {
                    var title = MessageBundle.message("action.azure.aad.registerApp.registeringApplication");
                    var task = new RegisterApplicationTask(project, dialog.getForm().getData(), client, subscription);

                    AzureTaskManager.getInstance().runInBackground(title, task);
                }
            });
        });
    }

    private static class RegisterApplicationTask implements Runnable {
        private final Project project;
        @NotNull
        private final ApplicationRegistrationModel model;
        @NotNull
        private final GraphRbacManagementClient graphClient;
        private final Subscription subscription;

        public RegisterApplicationTask(@NotNull Project project,
                                       @NotNull ApplicationRegistrationModel model,
                                       @NotNull GraphRbacManagementClient graphClient,
                                       @NotNull Subscription subscription) {
            this.project = project;
            this.model = model;
            this.graphClient = graphClient;
            this.subscription = subscription;
        }

        @Override
        public void run() {
            // create new application
            StringBuilder validSuffix = new StringBuilder();
            for (char c : (model.getDisplayName() + UUID.randomUUID().toString().substring(0, 6)).toCharArray()) {
                if (Character.isLetterOrDigit(c)) {
                    validSuffix.append(c);
                }
            }

            var params = new ApplicationCreateParameters();
            params.withDisplayName(model.getDisplayName());
            params.withIdentifierUris(Collections.singletonList("https://" + model.getDomain() + "/" + validSuffix));
            params.withReplyUrls(Collections.singletonList(model.getCallbackUrl()));
            params.withAvailableToOtherTenants(model.isMultiTenant());

            var application = graphClient.getApplications().create(params);

            // now display the new application in the "Application templates dialog"
            AzureTaskManager.getInstance().runLater(() -> {
                new AzureApplicationTemplateDialog(project, application, subscription).show();
            });
        }
    }
}
