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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.toolkit.intellij.function.FunctionAppCreationDialog;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.function.FunctionAppService;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.util.AzureLoginHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionModule;

@Name("Create Function App")
public class CreateFunctionAppAction extends NodeActionListener {
    private static final String ERROR_SIGNING_IN = "Error occurs on signing in";
    private final FunctionAppService functionAppService;
    private final FunctionModule functionModule;

    public CreateFunctionAppAction(FunctionModule functionModule) {
        super();
        this.functionModule = functionModule;
        this.functionAppService = FunctionAppService.getInstance();
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        final Project project = (Project) functionModule.getProject();
        try {
            if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project) ||
                !AzureLoginHelper.isAzureSubsAvailableOrReportError(ERROR_SIGNING_IN)) {
                return;
            }
        } catch (final Exception ex) {
            AzurePlugin.log(ERROR_SIGNING_IN, ex);
            DefaultLoader.getUIHelper().showException(ERROR_SIGNING_IN, ex, ERROR_SIGNING_IN, false, true);
        }
        final FunctionAppCreationDialog dialog = new FunctionAppCreationDialog(project);
        dialog.setOkActionListener((data) -> this.createFunctionApp(data, () -> DefaultLoader.getIdeHelper().invokeLater(dialog::close), project));
        dialog.show();
    }

    private void createFunctionApp(final FunctionAppConfig config, Runnable callback, final Project project) {
        final Task.Modal task = new Task.Modal(null, "Creating New Function App...", true) {
            @Override
            public void run(ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    final FunctionApp functionApp = functionAppService.createFunctionApp(config);
                    callback.run();
                    refreshAzureExplorer(functionApp);
                } catch (final Exception ex) {
                    // TODO: @wangmi show error with balloon notification instead of dialog
                    DefaultLoader.getUIHelper().showError("Error occurred on creating Function App: " + ex.getMessage(), "Create Function Failed");
                }
            }
        };
        ProgressManager.getInstance().run(task);
    }

    private void refreshAzureExplorer(FunctionApp functionApp) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (AzureUIRefreshCore.listeners != null) {
                AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, functionApp));
            }
        });
    }
}
