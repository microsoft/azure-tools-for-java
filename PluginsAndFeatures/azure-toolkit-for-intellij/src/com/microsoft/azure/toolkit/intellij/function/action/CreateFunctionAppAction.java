/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.intellij.function.action;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.toolkit.intellij.function.FunctionAppCreationDialog;
import com.microsoft.azure.toolkit.lib.common.handler.AzureExceptionHandler;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.function.FunctionAppService;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.intellij.util.AzureLoginHelper;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionModule;
import rx.Single;

import java.util.Objects;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

@Name("Create Function App")
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
    @AzureOperation(value = "open function app creation dialog", type = AzureOperation.Type.ACTION)
    public void actionPerformed(NodeActionEvent e) {
        final Project project = (Project) functionModule.getProject();
        if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project) ||
            !AzureLoginHelper.isAzureSubsAvailableOrReportError(message("common.error.signIn"))) {
            return;
        }
        openDialog(project, null);
    }

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
                    final AzureExceptionHandler.AzureExceptionAction action = AzureExceptionHandler.AzureExceptionAction.simple(
                        String.format("Reopen dialog \"%s\"", dialog.getTitle()),
                        t -> AzureTaskManager.getInstance().runLater("open dialog", () -> this.openDialog(project, config)));
                    AzureExceptionHandler.notify(error, action);
                });
        });
        dialog.show();
    }

    @AzureOperation(value = "create function app", type = AzureOperation.Type.ACTION)
    private Single<FunctionApp> createFunctionApp(final FunctionAppConfig config) {
        final AzureTask<FunctionApp> task = new AzureTask<>(null, message("function.create.task.title"), true, () -> {
            final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            indicator.setIndeterminate(true);
            return functionAppService.createFunctionApp(config);
        });
        return AzureTaskManager.getInstance().runInModal(task).toSingle().doOnSuccess(app -> {
            this.notifyCreationSuccess(app);
            this.refreshAzureExplorer(app);
        });
    }

    @AzureOperation(value = "refresh azure explorer", type = AzureOperation.Type.TASK)
    private void refreshAzureExplorer(FunctionApp app) {
        AzureTaskManager.getInstance().runLater(() -> {
            if (AzureUIRefreshCore.listeners != null) {
                AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, app));
            }
        });
    }

    private void notifyCreationSuccess(final FunctionApp app) {
        final String title = message("function.create.task.success.notification.title");
        final String message = String.format(message("function.create.task.success.notification.message"), app.name());
        final Notification notification = new Notification(NOTIFICATION_GROUP_ID, title, message, NotificationType.INFORMATION);
        Notifications.Bus.notify(notification);
    }
}
