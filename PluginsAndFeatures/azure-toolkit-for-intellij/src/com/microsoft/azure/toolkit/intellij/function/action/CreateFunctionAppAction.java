/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.action;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessager;
import com.microsoft.azure.toolkit.intellij.function.FunctionAppCreationDialog;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExceptionHandler;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExceptionHandler.AzureExceptionAction;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperationTitle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.function.FunctionAppService;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.intellij.actions.AzureSignInAction;
import com.microsoft.intellij.util.AzureLoginHelper;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionModule;
import rx.Single;

import java.util.Objects;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle.title;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

@Name("Create")
public class CreateFunctionAppAction extends NodeActionListener {

    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";
    private final FunctionAppService functionAppService;
    private final FunctionModule functionModule;

    public CreateFunctionAppAction(FunctionModule functionModule) {
        super();
        this.functionModule = functionModule;
        this.functionAppService = FunctionAppService.getInstance();
    }

    @Override
    public AzureActionEnum getAction() {
        return AzureActionEnum.CREATE;
    }

    @Override
    @AzureOperation(name = "function.create", type = AzureOperation.Type.ACTION)
    public void actionPerformed(NodeActionEvent e) {
        final Project project = (Project) functionModule.getProject();
        AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project).subscribe((isLoggedIn) -> {
            if (isLoggedIn && AzureLoginHelper.isAzureSubsAvailableOrReportError(message("common.error.signIn"))) {
                openDialog(project, null);
            }
        });
    }

    @AzureOperation(name = "function.open_creation_dialog", type = AzureOperation.Type.ACTION)
    private void openDialog(final Project project, @Nullable final FunctionAppConfig data) {
        final FunctionAppCreationDialog dialog = new FunctionAppCreationDialog(project);
        if (Objects.nonNull(data)) {
            dialog.setData(data);
        }
        dialog.setOkActionListener((config) -> {
            dialog.close();
            this.createFunctionApp(config)
                .subscribe(functionApp -> {
                }, (error) -> {
                    final String title = String.format("Reopen dialog \"%s\"", dialog.getTitle());
                    final Consumer<Throwable> act = t -> AzureTaskManager.getInstance().runLater("open dialog", () -> this.openDialog(project, config));
                    final AzureExceptionAction action = AzureExceptionAction.simple(title, act);
                    AzureExceptionHandler.notify(error, action);
                });
        });
        dialog.show();
    }

    @AzureOperation(name = "function.create_detail", params = {"config.getName()"}, type = AzureOperation.Type.ACTION)
    private Single<FunctionApp> createFunctionApp(final FunctionAppConfig config) {
        final IAzureOperationTitle title = title("function.create_detail", config.getName());
        final IntellijAzureMessager actionMessenger = new IntellijAzureMessager() {
            @Override
            public void info(@NotNull String message, String title) {
                // swallow info notification for action
            }
        };
        final AzureTask<FunctionApp> task = new AzureTask<>(null, title, false, () -> {
            AzureMessager.getContext().setMessager(actionMessenger);
            final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            indicator.setIndeterminate(true);
            return functionAppService.createFunctionApp(config);
        });
        return AzureTaskManager.getInstance().runInModalAsObservable(task).toSingle().doOnSuccess(app -> {
            AzureMessager.getMessager().success(message("function.create.success.message", app.name()), message("function.create.success.title"));
            this.refreshAzureExplorer(app);
        });
    }

    @AzureOperation(name = "common|explorer.refresh", type = AzureOperation.Type.TASK)
    private void refreshAzureExplorer(FunctionApp app) {
        AzureTaskManager.getInstance().runLater(() -> {
            if (AzureUIRefreshCore.listeners != null) {
                AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, app));
            }
        });
    }
}
