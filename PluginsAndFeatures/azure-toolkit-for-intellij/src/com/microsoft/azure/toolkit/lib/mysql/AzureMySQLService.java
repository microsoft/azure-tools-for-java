/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.mysql.v2020_01_01.Server;
import com.microsoft.azure.management.mysql.v2020_01_01.ServerPropertiesForDefaultCreate;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.toolkit.lib.appservice.Draft;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.*;

import java.util.Collections;
import java.util.Map;

public class AzureMySQLService {
    private static final AzureMySQLService instance = new AzureMySQLService();

    public static AzureMySQLService getInstance() {
        return AzureMySQLService.instance;
    }

    @AzureOperation(
        name = "mysql.create",
        params = {
            "$config.getServerName()",
            "$config.getSubscription().displayName()"
        },
        type = AzureOperation.Type.SERVICE
    )
    public Server createMySQL(final AzureMySQLConfig config) {
        final Operation operation = TelemetryManager.createOperation(ActionConstants.MySQL.CREATE);
        try {
            operation.start();
            final String subscriptionId = config.getSubscription().subscriptionId();
            EventUtil.logEvent(EventType.info, operation, Collections.singletonMap(TelemetryConstants.SUBSCRIPTIONID, subscriptionId));
            // create resource group if necessary.
            if (config.getResourceGroup() instanceof Draft) {
                Azure azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId);
                ResourceGroup newResourceGroup = azure.resourceGroups().define(config.getResourceGroup().name()).withRegion(config.getRegion()).create();
                config.setResourceGroup(newResourceGroup);
            }
            // create mysql server
            ServerPropertiesForDefaultCreate parameters = new ServerPropertiesForDefaultCreate();
            parameters.withAdministratorLogin(config.getAdminUsername())
                    .withAdministratorLoginPassword(String.valueOf(config.getPassword()))
                    .withVersion(config.getVersion());
            Server server = MySQLMvpModel.create(subscriptionId, config.getResourceGroup().name(), config.getServerName(), config.getRegion(), parameters);
            // update access from azure services
            MySQLMvpModel.FirewallRuleMvpModel.updateAllowAccessFromAzureServices(subscriptionId, server, config.isAllowAccessFromAzureServices());
            // update access from local machine
            MySQLMvpModel.FirewallRuleMvpModel.updateAllowAccessToLocalMachine(subscriptionId, server, config.isAllowAccessFromLocalMachine());
            return server;
        } catch (final RuntimeException e) {
            EventUtil.logError(operation, ErrorType.systemError, e, null, null);
            throw e;
        } finally {
            operation.complete();
        }
    }

}
