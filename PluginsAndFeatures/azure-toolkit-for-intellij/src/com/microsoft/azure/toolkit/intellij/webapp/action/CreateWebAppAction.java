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

package com.microsoft.azure.toolkit.intellij.webapp.action;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.toolkit.intellij.webapp.WebAppCreationDialog;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.webapp.WebAppConfig;
import com.microsoft.azure.toolkit.lib.webapp.WebAppService;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.runner.RunProcessHandler;
import com.microsoft.intellij.util.AzureLoginHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule;

import java.nio.file.Path;
import java.util.Objects;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

@Name("Create Web App")
public class CreateWebAppAction extends NodeActionListener {
    private final WebAppService webappService;
    private final WebAppModule webappModule;

    public CreateWebAppAction(WebAppModule webappModule) {
        super();
        this.webappModule = webappModule;
        this.webappService = WebAppService.getInstance();
    }

    @Override
    @AzureOperation(value = "start to create a new web app", type = AzureOperation.Type.ACTION)
    public void actionPerformed(NodeActionEvent e) {
        final Project project = (Project) webappModule.getProject();
        try {
            if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project) ||
                !AzureLoginHelper.isAzureSubsAvailableOrReportError(message("common.error.signIn"))) {
                return;
            }
        } catch (final Exception ex) {
            AzurePlugin.log(message("common.error.signIn"), ex);
            DefaultLoader.getUIHelper().showException(message("common.error.signIn"), ex, message("common.error.signIn"), false, true);
        }
        final WebAppCreationDialog dialog = new WebAppCreationDialog(project);
        dialog.setOkActionListener((data) -> this.createWebApp(data, () -> DefaultLoader.getIdeHelper().invokeLater(dialog::close), project));
        dialog.show();
    }

    @AzureOperation(
        value = "create web app[%s, os=%s, rg=%s, sp=%s] in subscription[%s]",
        params = {
            "$config.getName()",
            "$config.getPlatform().getOs()",
            "$config.getResourceGroup().name()",
            "$config.getServicePlan().name()",
            "$config.getSubscription().displayName()"
        },
        type = AzureOperation.Type.ACTION
    )
    private void createWebApp(final WebAppConfig config, Runnable callback, final Project project) {
        final AzureTask task = new AzureTask(null, message("webapp.create.task.title"), true, () -> {
            ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
            try {
                final WebApp webapp = webappService.createWebApp(config);
                callback.run();
                refreshAzureExplorer();
                final Path application = config.getApplication();
                if (Objects.nonNull(application) && application.toFile().exists()) {
                    DefaultLoader.getIdeHelper().invokeLater(() -> deploy(webapp, application, project));
                }
            } catch (final Exception ex) {
                // TODO: @wangmi show error with balloon notification instead of dialog
                DefaultLoader.getUIHelper().showError(message("webapp.create.error.title") + ex.getMessage(), message("webapp.create.error.createFailed"));
            }
        });
        AzureTaskManager.getInstance().runInModal(task);
    }

    @AzureOperation(
        value = "deploy artifact[%s] to web app[%s]",
        params = {"$application.getFileName().toString()", "$config.getName()"},
        type = AzureOperation.Type.ACTION
    )
    private void deploy(final WebApp webapp, final Path application, final Project project) {
        final AzureTask task = new AzureTask(null, message("webapp.deploy.task.title"), true, () -> {
            ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
            try {
                final RunProcessHandler processHandler = new RunProcessHandler();
                processHandler.addDefaultListener();
                final ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
                processHandler.startNotify();
                consoleView.attachToProcess(processHandler);
                WebAppUtils.deployArtifactsToAppService(webapp, application.toFile(), true, processHandler);
            } catch (final Exception ex) {
                // TODO: @wangmi show error with balloon notification instead of dialog
                DefaultLoader.getUIHelper().showError(message("webapp.deploy.error.title") + ex.getMessage(), message("webapp.deploy.error.deployFailed"));
            }
        });
        AzureTaskManager.getInstance().runInModal(task);
    }

    @AzureOperation(
        value = "refresh azure explorer",
        type = AzureOperation.Type.ACTION
    )
    private void refreshAzureExplorer() {
        AzureTaskManager.getInstance().runLater(() -> {
            if (AzureUIRefreshCore.listeners != null) {
                AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, null));
            }
        });
    }
}
