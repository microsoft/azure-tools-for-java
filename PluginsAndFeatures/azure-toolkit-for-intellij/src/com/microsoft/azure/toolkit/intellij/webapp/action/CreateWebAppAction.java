/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp.action;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.webapp.WebAppCreationDialog;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.SimpleMessageAction;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperationTitle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.webapp.WebAppConfig;
import com.microsoft.azure.toolkit.lib.webapp.WebAppService;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.intellij.RunProcessHandler;
import com.microsoft.intellij.actions.AzureSignInAction;
import com.microsoft.intellij.util.AzureLoginHelper;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule;
import rx.Single;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle.title;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class CreateWebAppAction extends NodeActionListener {
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";
    private final WebAppService webappService;
    private final WebAppModule webappModule;

    public CreateWebAppAction(WebAppModule webappModule) {
        super();
        this.webappModule = webappModule;
        this.webappService = WebAppService.getInstance();
    }

    @Override
    public AzureActionEnum getAction() {
        return AzureActionEnum.CREATE;
    }

    @Override
    @AzureOperation(name = "webapp.create", type = AzureOperation.Type.ACTION)
    public void actionPerformed(NodeActionEvent e) {
        final Project project = (Project) webappModule.getProject();
        AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project).subscribe((isLoggedIn) -> {
            if (isLoggedIn && AzureLoginHelper.isAzureSubsAvailableOrReportError(message("common.error.signIn"))) {
                this.openDialog(project, null);
            }
        });
    }

    @AzureOperation(name = "webapp.open_creation_dialog", type = AzureOperation.Type.ACTION)
    private void openDialog(final Project project, @Nullable final WebAppConfig data) {
        final WebAppCreationDialog dialog = new WebAppCreationDialog(project);
        if (Objects.nonNull(data)) {
            dialog.setData(data);
        }
        dialog.setOkActionListener((config) -> {
            dialog.close();
            this.createWebApp(config)
                .subscribe(webapp -> {
                    final Path artifact = config.getApplication();
                    if (Objects.nonNull(artifact) && artifact.toFile().exists()) {
                        AzureTaskManager.getInstance().runLater("deploy", () -> deploy(webapp, artifact, project));
                    }
                }, (error) -> {
                    final String title = String.format("Reopen dialog \"%s\"", dialog.getTitle());
                    final Consumer<IAzureMessage> act = t -> AzureTaskManager.getInstance().runLater("open dialog", () -> this.openDialog(project, config));
                    final SimpleMessageAction action = new SimpleMessageAction(title, act);
                    AzureMessager.getMessager().error(error, null, action);
                });
        });
        dialog.show();
    }

    @AzureOperation(name = "webapp.create_detail", params = {"config.getName()"}, type = AzureOperation.Type.ACTION)
    private Single<IWebApp> createWebApp(final WebAppConfig config) {
        final IAzureOperationTitle title = title("webapp.create_detail", config.getName());
        final AzureTask<IWebApp> task = new AzureTask<>(null, title, false, () -> {
            final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            indicator.setIndeterminate(true);
            return webappService.createWebApp(config);
        });
        return AzureTaskManager.getInstance().runInModalAsObservable(task).toSingle().doOnSuccess(app -> {
            this.notifyCreationSuccess(app);
            this.refreshAzureExplorer(app);
        });
    }

    @AzureOperation(name = "webapp.deploy_artifact", params = {"webapp.name()"}, type = AzureOperation.Type.ACTION)
    private void deploy(final IWebApp webapp, final Path application, final Project project) {
        final IAzureOperationTitle title = title("webapp.deploy_artifact", webapp.name());
        final AzureTask<Void> task = new AzureTask<>(null, title, false, () -> {
            ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
            final RunProcessHandler processHandler = new RunProcessHandler();
            processHandler.addDefaultListener();
            final ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
            processHandler.startNotify();
            consoleView.attachToProcess(processHandler);
            AzureWebAppMvpModel.getInstance().deployArtifactsToWebApp(webapp, application.toFile(), true, processHandler);
        });
        AzureTaskManager.getInstance().runInModalAsObservable(task).single().subscribe((none) -> {
            this.notifyDeploymentSuccess(webapp);
        }); // let root exception handler to show the error.
    }

    @AzureOperation(name = "common|explorer.refresh", type = AzureOperation.Type.TASK)
    private void refreshAzureExplorer(IWebApp app) {
        AzureTaskManager.getInstance().runLater(() -> {
            if (AzureUIRefreshCore.listeners != null) {
                AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, null));
            }
        });
    }

    private void notifyCreationSuccess(final IWebApp app) {
        final String title = message("webapp.create.success.title");
        final String message = message("webapp.create.success.message", app.name());
        final Notification notification = new Notification(NOTIFICATION_GROUP_ID, title, message, NotificationType.INFORMATION);
        Notifications.Bus.notify(notification);
    }

    private void notifyDeploymentSuccess(final IWebApp app) {
        final String title = message("webapp.deploy.success.title");
        final String message = message("webapp.deploy.success.message", app.name());
        final Notification notification = new Notification(NOTIFICATION_GROUP_ID, title, message, NotificationType.INFORMATION);
        Notifications.Bus.notify(notification);
    }
}
