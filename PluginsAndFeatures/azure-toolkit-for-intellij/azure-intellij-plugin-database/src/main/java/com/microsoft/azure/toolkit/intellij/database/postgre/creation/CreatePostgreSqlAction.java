/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.postgre.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.Draft;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.postgre.AzurePostgreSql;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;
import com.microsoft.azure.toolkit.lib.postgre.model.PostgreSqlServerConfig;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;

import javax.annotation.Nonnull;

public class CreatePostgreSqlAction {
    public static void create(Project project) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final PostgreSqlCreationDialog dialog = new PostgreSqlCreationDialog(project);
            dialog.setOkActionListener(config -> {
                doCreate(config, project);
                dialog.close();
            });
            dialog.show();
        });

    }

    @AzureOperation(name = "postgre|server.create", params = {"config.getServerName()"}, type = AzureOperation.Type.ACTION)
    private static void doCreate(final AzurePostgreSqlConfig config, final Project project) {
        final AzureString title = AzureOperationBundle.title("postgre|server.create", config.getServerName());
        AzureTaskManager.getInstance().runInBackground(title, () -> {
            final ResourceGroup rg = config.getResourceGroup();
            if (rg instanceof Draft) {
                new CreateResourceGroupTask(rg.getSubscriptionId(), rg.getName(), config.getRegion()).execute();
            }
            final PostgreSqlServer server = Azure.az(AzurePostgreSql.class).create(toPostgreSqlServerConfig(config)).commit();
            if (config.isAllowAccessFromAzureServices()) {
                server.firewallRules().enableAzureAccessRule();
            }
            // update access from local machine
            if (config.isAllowAccessFromLocalMachine()) {
                server.firewallRules().enableLocalMachineAccessRule(server.getPublicIpForLocalMachine());
            }
        });
    }

    private static PostgreSqlServerConfig toPostgreSqlServerConfig(@Nonnull AzurePostgreSqlConfig config) {
        return PostgreSqlServerConfig.builder()
                .subscription(config.getSubscription())
                .resourceGroup(config.getResourceGroup())
                .region(config.getRegion())
                .name(config.getServerName())
                .version(config.getVersion())
                .administratorLoginName(config.getAdminUsername())
                .administratorLoginPassword(String.valueOf(config.getPassword()))
                .build();
    }
}
