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

package com.microsoft.tooling.msservices.serviceexplorer.azure.mysql;

import com.microsoft.azure.management.mysql.v2020_01_01.Server;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshListener;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.util.List;

public class MySQLModule extends AzureRefreshableNode implements MySQLModuleView {

    // TODO (qianjin): updated once Tanya completes UI design.
    public static final String BASE_MODULE_NAME = "Azure Database for MySQL";
    public static final String ACTION_PATTERN_SUFFIX = BASE_MODULE_NAME + " (%s)...";
    private static final String MYSQL_DATABASE_MODULE_ID = MySQLModule.class.getName();

    public MySQLModule(final Node parent) {
        super(MYSQL_DATABASE_MODULE_ID, BASE_MODULE_NAME, parent, null);
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

