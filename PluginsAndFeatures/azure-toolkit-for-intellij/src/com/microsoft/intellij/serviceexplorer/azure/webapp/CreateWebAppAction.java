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

package com.microsoft.intellij.serviceexplorer.azure.webapp;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.appservice.intellij.view.webapp.WebAppCreationDialog;
import com.microsoft.azure.toolkit.appservice.webapp.WebAppConfig;
import com.microsoft.azure.toolkit.appservice.webapp.WebAppService;
import com.microsoft.azure.management.appservice.WebApp;
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
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule;

@Name("Create Web App")
public class CreateWebAppAction extends NodeActionListener {
    private static final String ERROR_SIGNING_IN = "Error occurs on signing in";
    private final WebAppService webappService;
    private final WebAppModule webappModule;

    public CreateWebAppAction(WebAppModule webappModule) {
        super();
        this.webappModule = webappModule;
        this.webappService = WebAppService.getInstance();
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        final Project project = (Project) webappModule.getProject();
        try {
            if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project) ||
                    !AzureLoginHelper.isAzureSubsAvailableOrReportError(ERROR_SIGNING_IN)) {
                return;
            }
        } catch (final Exception ex) {
            AzurePlugin.log(ERROR_SIGNING_IN, ex);
            DefaultLoader.getUIHelper().showException(ERROR_SIGNING_IN, ex, ERROR_SIGNING_IN, false, true);
        }
        final WebAppCreationDialog dialog = new WebAppCreationDialog(project);
        dialog.setOkActionListener((data) -> this.createWebApp(data, dialog::close));
        dialog.show();
    }

    private void createWebApp(final WebAppConfig config, Runnable callback) {
        final Task.Modal task = new Task.Modal(null, "Creating New Web App...", true) {
            @Override
            public void run(ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    final WebApp webapp = webappService.createWebApp(config);
                    callback.run();
                    refreshAzureExplorer();
                } catch (final Exception ex) {
                    DefaultLoader.getUIHelper().showError("Create WebApp Failed : " + ex.getMessage(), "Create WebApp Failed");
                }
            }
        };
        ProgressManager.getInstance().run(task);
    }

    private void refreshAzureExplorer() {
        ApplicationManager.getApplication().invokeLater(() -> {
            sendTelemetry(null);
            if (AzureUIRefreshCore.listeners != null) {
                AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, null));
            }
        });
    }
}
