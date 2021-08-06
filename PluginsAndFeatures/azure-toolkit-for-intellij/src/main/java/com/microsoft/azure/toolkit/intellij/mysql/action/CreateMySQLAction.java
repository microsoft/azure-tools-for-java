/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.mysql.action;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.Draft;
import com.microsoft.azure.toolkit.intellij.mysql.creation.MySQLCreationDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySQLConfig;
import com.microsoft.azure.toolkit.lib.mysql.service.AzureMySql;
import com.microsoft.azure.toolkit.lib.mysql.service.MySqlServer;
import com.microsoft.azure.toolkit.lib.resource.AzureGroup;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.actions.AzureSignInAction;
import com.microsoft.intellij.util.AzureLoginHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLModule;

import java.util.Collections;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

@Name("Create")
public class CreateMySQLAction extends NodeActionListener {

    private final MySQLModule model;

    public CreateMySQLAction(MySQLModule model) {
        super();
        this.model = model;
    }

    @Override
    public AzureActionEnum getAction() {
        return AzureActionEnum.CREATE;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        final Project project = (Project) model.getProject();
        AzureSignInAction.requireSignedIn(project, () -> doActionPerformed(true, project));
    }

    private void doActionPerformed(boolean isLoggedIn, Project project) {
        try {
            if (!isLoggedIn ||
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
            createMySQL(config);
        };
        final String progressMessage = Node.getProgressMessage(AzureActionEnum.CREATE.getDoingName(), MySQLModule.MODULE_NAME, config.getServerName());
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, progressMessage, false, runnable));
    }

    @AzureOperation(
        name = "mysql.create",
        params = {
            "config.getServerName()",
            "config.getSubscription().getName()"
        },
        type = AzureOperation.Type.SERVICE
    )
    public void createMySQL(final AzureMySQLConfig config) {
        final Operation operation = TelemetryManager.createOperation(ActionConstants.MySQL.CREATE);
        try {
            operation.start();
            final String subscriptionId = config.getSubscription().getId();
            EventUtil.logEvent(EventType.info, operation, Collections.singletonMap(TelemetryConstants.SUBSCRIPTIONID, subscriptionId));
            // create resource group if necessary.
            if (config.getResourceGroup() instanceof Draft) {
                try {
                    Azure.az(AzureGroup.class).get(subscriptionId, config.getResourceGroup().getName());
                } catch (Throwable ex) {
                    Azure.az(AzureGroup.class).subscription(subscriptionId).create(config.getResourceGroup().getName(), config.getRegion().getName());
                }
                config.setResourceGroup(Azure.az(AzureGroup.class).get(subscriptionId, config.getResourceGroup().getName()));
            }
            // create mysql server
            final MySqlServer server = Azure.az(AzureMySql.class).subscription(subscriptionId).create()
                .withResourceGroupName(config.getResourceGroup().getName())
                .withRegion(config.getRegion())
                .withName(config.getServerName())
                .withVersion(config.getVersion())
                .withAdministratorLogin(config.getAdminUsername())
                .withAdministratorLoginPassword(String.valueOf(config.getPassword())).commit();
            // update access from azure services
            if (config.isAllowAccessFromAzureServices()) {
                server.firewallRules().enableAzureAccessRule();
            }
            // update access from local machine
            if (config.isAllowAccessFromLocalMachine()) {
                server.firewallRules().enableLocalMachineAccessRule(server.getPublicIpForLocalMachine());
            }
        } catch (final RuntimeException e) {
            EventUtil.logError(operation, ErrorType.systemError, e, null, null);
            throw e;
        } finally {
            operation.complete();
        }
    }
}
