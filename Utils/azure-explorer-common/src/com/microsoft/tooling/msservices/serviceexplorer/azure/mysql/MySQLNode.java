/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.mysql;

import com.microsoft.azure.management.mysql.v2020_01_01.Server;
import com.microsoft.azure.management.mysql.v2020_01_01.ServerState;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.BasicActionBuilder;
import com.microsoft.tooling.msservices.serviceexplorer.Groupable;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.Sortable;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MySQLNode extends Node implements TelemetryProperties {

    private static final ServerState SERVER_UPDATING = ServerState.fromString("Updating");

    public static final int OPERATE_GROUP = Groupable.DEFAULT_GROUP + 2;
    public static final int SHOW_PROPERTIES_PRIORITY = Sortable.DEFAULT_PRIORITY + 1;
    public static final int CONNECT_TO_SERVER_PRIORITY = Sortable.DEFAULT_PRIORITY + 2;

    private NodeAction startAction;
    private NodeAction restartAction;
    private NodeAction stopAction;

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
        addAction(initActionBuilder(this::start).withAction(AzureActionEnum.START).withBackgroudable(true).build());
        addAction(initActionBuilder(this::stop).withAction(AzureActionEnum.STOP).withBackgroudable(true).build());
        addAction(initActionBuilder(this::restart).withAction(AzureActionEnum.RESTART).withBackgroudable(true).build());
        addAction(initActionBuilder(this::delete).withAction(AzureActionEnum.DELETE).withBackgroudable(true).withPromptable(true).build());
        addAction(initActionBuilder(this::openInPortal).withAction(AzureActionEnum.OPEN_IN_PORTAL).withBackgroudable(true).build());
        addAction(initActionBuilder(this::showProperties).withAction(AzureActionEnum.SHOW_PROPERTIES).build());
        initActions();
    }

    private BasicActionBuilder initActionBuilder(Runnable runnable) {
        return new BasicActionBuilder(runnable)
                .withModuleName(MySQLModule.MODULE_NAME)
                .withInstanceName(name);
    }

    @Override
    public List<NodeAction> getNodeActions() {
        if (startAction == null)
            startAction = getNodeActionByName(AzureActionEnum.START.getName());

        if (restartAction == null)
            restartAction = getNodeActionByName(AzureActionEnum.RESTART.getName());

        if (stopAction == null)
            stopAction = getNodeActionByName(AzureActionEnum.STOP.getName());

        List<NodeAction> nodeActions = super.getNodeActions();

        boolean updating = SERVER_UPDATING.equals(serverState);
        boolean running = ServerState.READY.equals(serverState);

        if (stopAction != null)
            stopAction.setEnabled(!updating && running);

        if (startAction != null)
            startAction.setEnabled(!updating && !running);

        if (restartAction != null)
            restartAction.setEnabled(!updating && running);

        if (running) {
            int startIndex = nodeActions.indexOf(startAction);
            nodeActions.remove(startAction);
            if (!nodeActions.contains(stopAction))
                nodeActions.add(startIndex, stopAction);
        } else {
            int stopIndex = nodeActions.indexOf(stopAction);
            nodeActions.remove(stopAction);
            if (!nodeActions.contains(startAction))
                nodeActions.add(stopIndex, startAction);
        }

        getNodeActionByName(AzureActionEnum.DELETE.getName()).setEnabled(!updating);

        return nodeActions;
    }

    private void refreshNode() {
        Server result = MySQLMvpModel.findServer(subscriptionId, server.resourceGroupName(), server.name());
        this.serverState = result.userVisibleState();
    }

    @AzureOperation(name = ActionConstants.MySQL.DELETE, type = AzureOperation.Type.ACTION)
    private void delete() {
        this.serverState = SERVER_UPDATING;
        this.getParent().removeNode(this.getSubscriptionId(), this.getId(), MySQLNode.this);
    }

    @AzureOperation(name = ActionConstants.MySQL.START, type = AzureOperation.Type.ACTION)
    private void start() {
        this.serverState = SERVER_UPDATING;
        MySQLMvpModel.start(this.getSubscriptionId(), this.getServer());
        this.refreshNode();
    }

    @AzureOperation(name = ActionConstants.MySQL.STOP, type = AzureOperation.Type.ACTION)
    private void stop() {
        this.serverState = SERVER_UPDATING;
        MySQLMvpModel.stop(this.getSubscriptionId(), this.getServer());
        this.refreshNode();
    }

    @AzureOperation(name = ActionConstants.MySQL.RESTART, type = AzureOperation.Type.ACTION)
    private void restart() {
        this.serverState = SERVER_UPDATING;
        MySQLMvpModel.restart(this.getSubscriptionId(), this.getServer());
        this.refreshNode();
    }

    @AzureOperation(name = ActionConstants.MySQL.OPEN_IN_PORTAL, type = AzureOperation.Type.ACTION)
    private void openInPortal() {
        this.openResourcesInPortal(this.subscriptionId, this.server.id());
    }

    @AzureOperation(name = ActionConstants.MySQL.SHOW_PROPERTIES, type = AzureOperation.Type.ACTION)
    private void showProperties() {
        DefaultLoader.getUIHelper().openMySQLPropertyView(MySQLNode.this);
    }

    @Override
    public Map<String, String> toProperties() {
        return Collections.singletonMap(TelemetryConstants.SUBSCRIPTIONID, this.subscriptionId);
    }
}
