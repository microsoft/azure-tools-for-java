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

import com.microsoft.azure.management.mysql.v2017_12_01.Server;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;
import com.microsoft.azuretools.core.mvp.model.springcloud.SpringCloudIdHelper;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.util.List;

public class MySQLModule extends AzureRefreshableNode implements MySQLModuleView {

    protected static final String ICON_FILE = "azure-mysql-small.png";
    private static final String MYSQL_DATABASE_MODULE_ID = MySQLModule.class.getName();
    private static final String BASE_MODULE_NAME = "My SQL Database";
    private final MySQLModulePresenter mysqlModulePresenter;

    public MySQLModule(final Node parent) {
        super(MYSQL_DATABASE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_FILE);
        mysqlModulePresenter = new MySQLModulePresenter();
        mysqlModulePresenter.onAttachView(this);
    }

    public void renderChildren(List<Server> servers) {
        for (final Server server : servers) {
            final MySQLNode node = new MySQLNode(this,
                                                 SpringCloudIdHelper.getSubscriptionId(server.id()),
                                                 server);
            addChildNode(node);
        }
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        mysqlModulePresenter.onMySqlRefresh();
    }

    class MySQLModulePresenter extends MvpPresenter<MySQLModule> {
        private static final String FAILED_TO_LOAD_CLUSTERS = "Failed to load MySQL servers.";
        private static final String ERROR_LOAD_CLUSTER = "Azure Services Explorer - Error Loading MySQL Servers";

        public void onMySqlRefresh() {
            final MySQLModuleView view = getMvpView();
            if (view != null) {
                view.renderChildren(MySQLMvpModel.listAllMySQLServers());
            }
        }
    }
}

