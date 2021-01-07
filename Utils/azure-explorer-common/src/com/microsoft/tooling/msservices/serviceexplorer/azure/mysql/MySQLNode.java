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
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;
import com.microsoft.azuretools.telemetry.TelemetryParameter;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.*;
import com.microsoft.tooling.msservices.serviceexplorer.listener.ActionBackgroundable;
import com.microsoft.tooling.msservices.serviceexplorer.listener.ActionPromptable;
import com.microsoft.tooling.msservices.serviceexplorer.listener.ActionTelemetrable;
import lombok.Getter;

import java.util.List;

public class MySQLNode extends Node {

    private static final ServerState SERVER_UPDATING = ServerState.fromString("Updating");

    public static final int OPERATE_GROUP = Groupable.DEFAULT_GROUP + 2;
    public static final int SHOW_PROPERTIES_PRIORITY = Sortable.DEFAULT_PRIORITY + 1;
    public static final int CONNECT_TO_SERVER_PRIORITY = Sortable.DEFAULT_PRIORITY + 2;

    @Getter
    private final String subscriptionId;
    @Getter
    private final Server server;
    private ServerState serverState;

    public MySQLNode(AzureRefreshableNode parent, String subscriptionId, Server server) {
        super(server.id(), server.name(), parent, true);
        this.subscriptionId = subscriptionId;
        this.server = server;
        this.serverState = server.userVisibleState();
        loadActions();
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        boolean running = ServerState.READY.equals(serverState);
        boolean updating = SERVER_UPDATING.equals(serverState);
        return running ? AzureIconSymbol.MySQL.RUNNING : updating ? AzureIconSymbol.MySQL.UPDATING : AzureIconSymbol.MySQL.STOPPED;
    }

    @Override
    protected void loadActions() {
        addAction(new StartAction().asGenericListener(AzureActionEnum.START));
        addAction(new StopAction().asGenericListener(AzureActionEnum.STOP));
        addAction(new RestartAction().asGenericListener(AzureActionEnum.RESTART));
        addAction(new DeleteAction().asGenericListener(AzureActionEnum.DELETE));
        addAction(new OpenInPortalAction().asGenericListener(AzureActionEnum.OPEN_IN_PORTAL));
        addAction(new ShowPropertiesAction().asGenericListener(AzureActionEnum.SHOW_PROPERTIES));
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

    // Delete action class
    private class DeleteAction extends NodeActionListener implements ActionBackgroundable, ActionPromptable, ActionTelemetrable {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            MySQLNode.this.serverState = SERVER_UPDATING;
            MySQLNode.this.getParent().removeNode(MySQLNode.this.getSubscriptionId(), MySQLNode.this.getId(), MySQLNode.this);
        }

        @Override
        public String getPromptMessage() {
            return Node.getPromptMessage(AzureActionEnum.DELETE.getName().toLowerCase(), MySQLModule.MODULE_NAME, MySQLNode.this.name);
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.DELETE.getDoingName(), MySQLModule.MODULE_NAME, MySQLNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.MySQL.DELETE;
        }
    }

    // Start action class
    private class StartAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            MySQLNode.this.serverState = SERVER_UPDATING;
            MySQLMvpModel.start(MySQLNode.this.getSubscriptionId(), MySQLNode.this.getServer());
            MySQLNode.this.refreshNode();
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.START.getDoingName(), MySQLModule.MODULE_NAME, MySQLNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.MySQL.START;
        }
    }

    // Stop action class
    private class StopAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            MySQLNode.this.serverState = SERVER_UPDATING;
            MySQLMvpModel.stop(MySQLNode.this.getSubscriptionId(), MySQLNode.this.getServer());
            MySQLNode.this.refreshNode();
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.STOP.getDoingName(), MySQLModule.MODULE_NAME, MySQLNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.MySQL.STOP;
        }
    }

    // Restart action class
    private class RestartAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            MySQLNode.this.serverState = SERVER_UPDATING;
            MySQLMvpModel.restart(MySQLNode.this.getSubscriptionId(), MySQLNode.this.getServer());
            MySQLNode.this.refreshNode();
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.RESTART.getDoingName(), MySQLModule.MODULE_NAME, MySQLNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.MySQL.RESTART;
        }
    }

    // Open in portal action class
    private class OpenInPortalAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            MySQLNode.this.openResourcesInPortal(MySQLNode.this.subscriptionId, MySQLNode.this.server.id());
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.OPEN_IN_PORTAL.getDoingName(), MySQLModule.MODULE_NAME, MySQLNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.MySQL.OPEN_IN_PORTAL;
        }
    }

    // Show Properties
    private class ShowPropertiesAction extends NodeActionListener implements ActionTelemetrable {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            DefaultLoader.getUIHelper().openMySQLPropertyView(MySQLNode.this);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.MySQL.SHOW_PROPERTIES;
        }
    }

}
