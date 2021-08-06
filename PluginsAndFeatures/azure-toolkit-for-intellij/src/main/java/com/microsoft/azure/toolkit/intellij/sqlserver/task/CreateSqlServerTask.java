/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.sqlserver.task;

import com.microsoft.azure.toolkit.intellij.common.Draft;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.resource.AzureGroup;
import com.microsoft.azure.toolkit.lib.sqlserver.SqlServerConfig;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlServerEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.service.AzureSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.service.ISqlServer;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;

import java.util.Collections;

public class CreateSqlServerTask {

    private final SqlServerConfig config;

    public CreateSqlServerTask(SqlServerConfig config) {
        this.config = config;
    }

    @AzureOperation(
        name = "sqlserver|server.create",
        params = {
            "config.getServerName()",
            "config.getSubscription().displayName()"
        },
        type = AzureOperation.Type.SERVICE
    )
    public ISqlServer execute() {
        final Operation operation = TelemetryManager.createOperation(ActionConstants.MySQL.CREATE);
        try {
            operation.start();
            final String subscriptionId = config.getSubscription().getId();
            EventUtil.logEvent(EventType.info, operation, Collections.singletonMap(TelemetryConstants.SUBSCRIPTIONID, subscriptionId));
            // create resource group if necessary.
            if (config.getResourceGroup() instanceof Draft) {
                ResourceGroup newResourceGroup = Azure.az(AzureGroup.class)
                    .subscription(subscriptionId).create(config.getResourceGroup().getName(), config.getRegion().getName());
                config.setResourceGroup(newResourceGroup);
            }

            // create sql server
            SqlServerEntity entity = this.fromConfig(this.config);
            return Azure.az(AzureSqlServer.class).sqlServer(entity).create().withAdministratorLoginPassword(String.valueOf(config.getPassword())).commit();
        } catch (final RuntimeException e) {
            EventUtil.logError(operation, ErrorType.systemError, e, null, null);
            throw e;
        } finally {
            operation.complete();
        }
    }

    private SqlServerEntity fromConfig(SqlServerConfig config) {
        return SqlServerEntity.builder().name(config.getServerName()).subscriptionId(config.getSubscription().getId())
                .resourceGroup(config.getResourceGroup().getName()).region(Region.fromName(config.getRegion().getName()))
                .administratorLoginName(config.getAdminUsername())
                .enableAccessFromAzureServices(config.isAllowAccessFromAzureServices()).enableAccessFromLocalMachine(config.isAllowAccessFromLocalMachine())
                .build();
    }

}
