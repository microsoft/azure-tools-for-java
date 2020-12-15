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

package com.microsoft.azure.toolkit.lib.mysql;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.mysql.v2020_01_01.Server;
import com.microsoft.azure.management.mysql.v2020_01_01.ServerPropertiesForDefaultCreate;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.toolkit.lib.appservice.Draft;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.MYSQL;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.MYSQL_CREATE;

public class AzureMySQLService {
    private static final AzureMySQLService instance = new AzureMySQLService();

    public static AzureMySQLService getInstance() {
        return AzureMySQLService.instance;
    }

    @AzureOperation(
        value = "create Azure Database for MySQL[%s, rg=%s] in subscription[%s]",
        params = {
            "$config.getServerName()",
            "$config.getResourceGroup().name()",
            "$config.getSubscription().displayName()"
        },
        type = AzureOperation.Type.SERVICE
    )
    public Server createMySQL(final AzureMySQLConfig config) {
        final Operation operation = TelemetryManager.createOperation(MYSQL, MYSQL_CREATE);
        try {
            operation.start();
            String subscrptionId = config.getSubscription().subscriptionId();
            // create resource group if necessary.
            if (config.getResourceGroup() instanceof Draft) {
                Azure azure = AuthMethodManager.getInstance().getAzureClient(subscrptionId);
                ResourceGroup newResourceGroup = azure.resourceGroups().define(config.getResourceGroup().name()).withRegion(config.getRegion()).create();
                config.setResourceGroup(newResourceGroup);
            }
            // create mysql server
            ServerPropertiesForDefaultCreate parameters = new ServerPropertiesForDefaultCreate();
            parameters.withAdministratorLogin(config.getAdminUsername())
                    .withAdministratorLoginPassword(String.valueOf(config.getPassword()))
                    .withVersion(config.getVersion());
            Server server = MySQLMvpModel.create(subscrptionId, config.getResourceGroup().name(), config.getServerName(), config.getRegion(), parameters);
            // update access from azure services
            MySQLMvpModel.FirewallRuleMvpModel.updateAllowAccessFromAzureServices(subscrptionId, server, config.isAllowAccessFromAzureServices());
            // update access from local machine
            MySQLMvpModel.FirewallRuleMvpModel.updateAllowAccessToLocalMachine(subscrptionId, server, config.isAllowAccessFromLocalMachine());
            return server;
        } catch (final RuntimeException e) {
            throw e;
        } finally {
            operation.complete();
        }
    }

}
