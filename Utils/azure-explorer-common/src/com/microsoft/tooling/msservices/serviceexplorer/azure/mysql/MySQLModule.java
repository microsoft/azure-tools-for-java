/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.mysql;

import com.microsoft.azure.management.mysql.v2020_01_01.Server;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshListener;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.util.List;

public class MySQLModule extends AzureRefreshableNode implements MySQLModuleView {

    public static final String MODULE_NAME = "Azure Database for MySQL";
    private static final String MYSQL_DATABASE_MODULE_ID = MySQLModule.class.getName();

    public MySQLModule(final Node parent) {
        super(MYSQL_DATABASE_MODULE_ID, MODULE_NAME, parent);
        createListener();
    }

    public void renderChildren(List<Server> servers) {
        for (final Server server : servers) {
            final String sid = AzureMvpModel.getSegment(server.id(), "subscriptions");
            final MySQLNode node = new MySQLNode(this, sid, server);
            addChildNode(node);
        }
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.MySQL.MODULE;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        List<Server> items = MySQLMvpModel.listMySQLServers();
        this.renderChildren(items);
    }

    @Override
    public void removeNode(String sid, String id, Node node) {
        MySQLMvpModel.delete(sid, id);
        removeDirectChildNode(node);
    }

    private void createListener() {
        AzureUIRefreshListener listener = new AzureUIRefreshListener() {
            @Override
            public void run() {
                if (event.opsType == null) {
                    return;
                }
                switch (event.opsType) {
                    case SIGNIN:
                    case SIGNOUT:
                        removeAllChildNodes();
                        break;
                    case REFRESH:
                        if (isMySQLModuleEvent(event.object)) {
                            load(true);
                        }
                        break;
                    default:
                        if (isMySQLModuleEvent(event.object) && hasChildNodes()) {
                            load(true);
                        }
                        break;
                }
            }
        };
        AzureUIRefreshCore.addListener("MYSQL_MODULE", listener);
    }

    private boolean isMySQLModuleEvent(Object eventObject) {
        return eventObject != null && eventObject instanceof Server;
    }

}

