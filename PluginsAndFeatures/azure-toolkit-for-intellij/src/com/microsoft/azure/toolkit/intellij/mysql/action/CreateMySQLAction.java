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

package com.microsoft.azure.toolkit.intellij.mysql.action;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.mysql.v2020_01_01.Server;
import com.microsoft.azure.toolkit.intellij.mysql.creation.MySQLCreationDialog;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySQLConfig;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySQLService;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.util.AzureLoginHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLModule;

import javax.swing.*;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

@Name(CreateMySQLAction.ACTION_NAME)
public class CreateMySQLAction extends NodeActionListener {

    protected static final String ACTION_NAME = "Create";

    private final MySQLModule model;

    public CreateMySQLAction(MySQLModule model) {
        super();
        this.model = model;
    }

    @Override
    public Icon getIcon() {
        return DefaultLoader.getUIHelper().loadIconByAction(AzureActionEnum.CREATE);
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        final Project project = (Project) model.getProject();
        try {
            if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project) ||
                !AzureLoginHelper.isAzureSubsAvailableOrReportError(message("common.error.signIn"))) {
                return;
            }
        } catch (final Exception ex) {
            AzurePlugin.log(message("common.error.signIn"), ex);
            DefaultLoader.getUIHelper().showException(message("common.error.signIn"), ex, message("common.error.signIn"), false, true);
        }
        final MySQLCreationDialog dialog = new MySQLCreationDialog(project);
        dialog.setOkActionListener((data) -> this.createAzureMySQL(data, project, dialog));
        dialog.show();
    }

    private void createAzureMySQL(final AzureMySQLConfig config, final Project project, MySQLCreationDialog dialog) {
        final Runnable runnable = () -> {
            final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            indicator.setIndeterminate(true);
            DefaultLoader.getIdeHelper().invokeLater(dialog::close);
            Server server = AzureMySQLService.getInstance().createMySQL(config);
            refreshAzureExplorer(server);
        };
        final AzureTask task = new AzureTask(null, message("azure.mysql.create.task.title"), true, runnable);
        AzureTaskManager.getInstance().runInBackground(task);
    }

    @AzureOperation(value = "refresh azure explorer", type = AzureOperation.Type.TASK)
    private void refreshAzureExplorer(Server server) {
        AzureTaskManager.getInstance().runLater(() -> {
            if (AzureUIRefreshCore.listeners != null) {
                AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, server));
            }
        });
    }
}
