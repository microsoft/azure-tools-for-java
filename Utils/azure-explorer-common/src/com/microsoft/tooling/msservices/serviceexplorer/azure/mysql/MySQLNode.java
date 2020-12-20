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
import com.microsoft.azure.management.mysql.v2020_01_01.ServerState;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


public class MySQLNode extends Node {

    @Getter
    private final String subscriptionId;
    @Getter
    private final Server server;
    private ServerState serverState;

    public MySQLNode(AzureRefreshableNode parent, String subscriptionId, Server mysqlServer) {
        super(mysqlServer.id(), mysqlServer.name(), parent, null, true);
        this.subscriptionId = subscriptionId;
        this.server = mysqlServer;
        this.serverState = server.userVisibleState();
        loadActions();
    }

    @Override
    protected void loadActions() {
        initActions();
    }

    @Override
    public List<NodeAction> getNodeActions() {
        boolean running = ServerState.READY.equals(serverState);
        getNodeActionByName(StartAzureMySQLAction.ACTION_NAME).setEnabled(!running);
        getNodeActionByName(StopAzureMySQLAction.ACTION_NAME).setEnabled(running);
        getNodeActionByName(RestartAzureMySQLAction.ACTION_NAME).setEnabled(running);
        return super.getNodeActions();
    }

    private void refreshNode() {
        Server result = MySQLMvpModel.findServer(subscriptionId, server.resourceGroupName(), server.name());
        this.serverState = result.userVisibleState();
    }

    // Delete Azure MySQL action class
    @Name(DeleteAzureMySQLAction.ACTION_NAME)
    public class DeleteAzureMySQLAction extends AzureNodeActionPromptListener {

        private static final String ACTION_NAME = "Delete";
        private static final String ACTION_DOING = "Deleting";
        private static final String DELETE_PROMPT_MESSAGE =
                "This operation will delete your azure mysql server: %s." + StringUtils.LF + "Are you sure you want to continue?";

        public DeleteAzureMySQLAction() {
            super(MySQLNode.this, String.format(DELETE_PROMPT_MESSAGE, MySQLNode.this.name),
                    String.format(ACTION_DOING + StringUtils.SPACE + MySQLModule.ACTION_PATTERN_SUFFIX, MySQLNode.this.name));
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e) {
            MySQLNode.this.getParent().removeNode(MySQLNode.this.getSubscriptionId(), azureNode.getId(), azureNode);
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) {

        }
    }

    // Start Azure MySQL action class
    @Name(StartAzureMySQLAction.ACTION_NAME)
    public class StartAzureMySQLAction extends NodeActionListener {

        private static final String ACTION_NAME = "Start";
        private static final String ACTION_DOING = "Starting";

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            Runnable runnable = () -> {
                MySQLMvpModel.start(MySQLNode.this.getSubscriptionId(), MySQLNode.this.getServer());
                MySQLNode.this.refreshNode();
            };
            String taskTitle = String.format(ACTION_DOING + StringUtils.SPACE + MySQLModule.ACTION_PATTERN_SUFFIX, MySQLNode.this.name);
            AzureTaskManager.getInstance().runInBackground(new AzureTask(null, taskTitle, false, runnable));
        }
    }

    // Stop Azure MySQL action class
    @Name(StopAzureMySQLAction.ACTION_NAME)
    public class StopAzureMySQLAction extends NodeActionListener {

        private static final String ACTION_NAME = "Stop";
        private static final String ACTION_DOING = "Stopping";

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            Runnable runnable = () -> {
                MySQLMvpModel.stop(MySQLNode.this.getSubscriptionId(), MySQLNode.this.getServer());
                MySQLNode.this.refreshNode();
            };
            String taskTitle = String.format(ACTION_DOING + StringUtils.SPACE + MySQLModule.ACTION_PATTERN_SUFFIX, MySQLNode.this.name);
            AzureTaskManager.getInstance().runInBackground(new AzureTask(null, taskTitle, false, runnable));
        }
    }

    // Stop Azure MySQL action class
    @Name(RestartAzureMySQLAction.ACTION_NAME)
    public class RestartAzureMySQLAction extends NodeActionListener {

        private static final String ACTION_NAME = "Restart";
        private static final String ACTION_DOING = "Restarting";

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            Runnable runnable = () -> {
                MySQLMvpModel.restart(MySQLNode.this.getSubscriptionId(), MySQLNode.this.getServer());
                MySQLNode.this.refreshNode();
            };
            String taskTitle = String.format(ACTION_DOING + StringUtils.SPACE + MySQLModule.ACTION_PATTERN_SUFFIX, MySQLNode.this.name);
            AzureTaskManager.getInstance().runInBackground(new AzureTask(null, taskTitle, false, runnable));
        }
    }

    // Open in browser action class
    @Name(OpenInBrowserAction.ACTION_NAME)
    public class OpenInBrowserAction extends NodeActionListener {

        private static final String ACTION_NAME = "Open in Portal";
        private static final String ACTION_DOING = "Opening";

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            Runnable runnable = () -> MySQLNode.this.openResourcesInPortal(MySQLNode.this.subscriptionId, MySQLNode.this.server.id());
            String taskTitle = String.format(ACTION_DOING + StringUtils.SPACE + MySQLModule.ACTION_PATTERN_SUFFIX, MySQLNode.this.name);
            AzureTaskManager.getInstance().runInBackground(new AzureTask(null, taskTitle, false, runnable));
        }
    }
}
