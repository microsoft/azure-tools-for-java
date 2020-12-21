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
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;
import com.microsoft.tooling.msservices.serviceexplorer.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.List;

public class MySQLNode extends Node {

    private static final ServerState SERVER_UPDATING = ServerState.fromString("Updating");

    public static final int OPEN_GROUP = Groupable.DEFAULT_GROUP + 1;
    public static final int OPEN_IN_PORTAL_PRIORITY = Sortable.DEFAULT_PRIORITY + 1;

    public static final int OPERATE_GROUP = Groupable.DEFAULT_GROUP + 2;
    public static final int SHOW_PROPERTIES_PRIORITY = Sortable.DEFAULT_PRIORITY + 1;
    public static final int CONNECT_TO_SERVER_PRIORITY = Sortable.DEFAULT_PRIORITY + 2;

    @Getter
    private final String subscriptionId;
    @Getter
    private final Server server;
    private ServerState serverState;

    public MySQLNode(AzureRefreshableNode parent, String subscriptionId, Server server) {
        super(server.id(), server.name(), parent, null, true);
        this.subscriptionId = subscriptionId;
        this.server = server;
        this.serverState = server.userVisibleState();
        loadActions();
    }

    @Override
    public @Nullable Icon getIcon() {
        boolean running = ServerState.READY.equals(serverState);
        boolean updating = SERVER_UPDATING.equals(serverState);
        return this.getIconByState(running ? NodeState.RUNNING : updating ? NodeState.UPDATING : NodeState.STOPPED, NodeState.DEFAULT);
    }

    @Override
    protected void loadActions() {
        addAction(new BasicActionListener(new StartAzureMySQLAction(), AzureActionEnum.START));
        addAction(new BasicActionListener(new StopAzureMySQLAction(), AzureActionEnum.STOP));
        addAction(new BasicActionListener(new RestartAzureMySQLAction(), AzureActionEnum.RESTART));
        addAction(new BasicActionListener(new DeleteAzureMySQLAction(), AzureActionEnum.DELETE));
        addAction(new BasicActionListener(new DeleteAzureMySQLAction(), AzureActionEnum.OPEN_IN_PORTAL));
        initActions();
    }

    @Override
    public List<NodeAction> getNodeActions() {
        boolean updating = SERVER_UPDATING.equals(serverState);
        boolean running = ServerState.READY.equals(serverState);
        getNodeActionByName(AzureActionEnum.START.getName()).setEnabled(!updating && !running);
        getNodeActionByName(AzureActionEnum.STOP.getName()).setEnabled(!updating && running);
        getNodeActionByName(AzureActionEnum.RESTART.getName()).setEnabled(!updating && running);
        getNodeActionByName(AzureActionEnum.DELETE.getName()).setEnabled(!updating);
        return super.getNodeActions();
    }

    private void refreshNode() {
        Server result = MySQLMvpModel.findServer(subscriptionId, server.resourceGroupName(), server.name());
        this.serverState = result.userVisibleState();
    }

    // Delete Azure MySQL action class
    public class DeleteAzureMySQLAction extends AzureNodeActionPromptListener {

        private static final String ACTION_DOING = "Deleting";
        private static final String DELETE_PROMPT_MESSAGE =
                "This operation will delete your azure mysql server: %s." + StringUtils.LF + "Are you sure you want to continue?";

        public DeleteAzureMySQLAction() {
            super(MySQLNode.this, String.format(DELETE_PROMPT_MESSAGE, MySQLNode.this.name),
                    String.format(ACTION_DOING + StringUtils.SPACE + MySQLModule.ACTION_PATTERN_SUFFIX, MySQLNode.this.name));
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e) {
            MySQLNode.this.serverState = SERVER_UPDATING;
            MySQLNode.this.getParent().removeNode(MySQLNode.this.getSubscriptionId(), azureNode.getId(), azureNode);
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) {

        }
    }

    // Start Azure MySQL action class
    private class StartAzureMySQLAction extends NodeActionListener {

        private static final String ACTION_DOING = "Starting";

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            MySQLNode.this.serverState = SERVER_UPDATING;
            Runnable runnable = () -> {
                MySQLMvpModel.start(MySQLNode.this.getSubscriptionId(), MySQLNode.this.getServer());
                MySQLNode.this.refreshNode();
            };
            String taskTitle = String.format(ACTION_DOING + StringUtils.SPACE + MySQLModule.ACTION_PATTERN_SUFFIX, MySQLNode.this.name);
            AzureTaskManager.getInstance().runInBackground(new AzureTask(null, taskTitle, false, runnable));
        }

    }

    // Stop Azure MySQL action class
    private class StopAzureMySQLAction extends NodeActionListener {

        private static final String ACTION_DOING = "Stopping";

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            MySQLNode.this.serverState = SERVER_UPDATING;
            Runnable runnable = () -> {
                MySQLMvpModel.stop(MySQLNode.this.getSubscriptionId(), MySQLNode.this.getServer());
                MySQLNode.this.refreshNode();
            };
            String taskTitle = String.format(ACTION_DOING + StringUtils.SPACE + MySQLModule.ACTION_PATTERN_SUFFIX, MySQLNode.this.name);
            AzureTaskManager.getInstance().runInBackground(new AzureTask(null, taskTitle, false, runnable));
        }

    }

    // Restart Azure MySQL action class
    private class RestartAzureMySQLAction extends NodeActionListener {

        private static final String ACTION_DOING = "Restarting";

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            MySQLNode.this.serverState = SERVER_UPDATING;
            Runnable runnable = () -> {
                MySQLMvpModel.restart(MySQLNode.this.getSubscriptionId(), MySQLNode.this.getServer());
                MySQLNode.this.refreshNode();
            };
            String taskTitle = String.format(ACTION_DOING + StringUtils.SPACE + MySQLModule.ACTION_PATTERN_SUFFIX, MySQLNode.this.name);
            AzureTaskManager.getInstance().runInBackground(new AzureTask(null, taskTitle, false, runnable));
        }

    }

    // Open in browser action class
    private class OpenInBrowserAction extends NodeActionListener {

        private static final String ACTION_DOING = "Opening";

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            Runnable runnable = () -> MySQLNode.this.openResourcesInPortal(MySQLNode.this.subscriptionId, MySQLNode.this.server.id());
            String taskTitle = String.format(ACTION_DOING + StringUtils.SPACE + MySQLModule.ACTION_PATTERN_SUFFIX, MySQLNode.this.name);
            AzureTaskManager.getInstance().runInBackground(new AzureTask(null, taskTitle, false, runnable));
        }

    }
}
