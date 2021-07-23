/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.mysql;

import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.mysql.service.MySqlServer;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
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
import org.eclipse.jgit.util.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MySQLNode extends Node implements TelemetryProperties {

    private static final String SERVER_UPDATING = "Updating";

    public static final int OPERATE_GROUP = Groupable.DEFAULT_GROUP + 2;
    public static final int SHOW_PROPERTIES_PRIORITY = Sortable.DEFAULT_PRIORITY + 1;
    public static final int CONNECT_TO_SERVER_PRIORITY = Sortable.DEFAULT_PRIORITY + 2;

    @Getter
    private final MySqlServer server;
    private String serverState;

    public MySQLNode(AzureRefreshableNode parent, @Nonnull MySqlServer server) {
        super(server.id(), server.name(), parent, true);
        this.server = server;
        this.serverState = server.entity().getState();
        loadActions();
        AzureEventBus.after("mysql|server.start", this::onMySqlServerStatusChanged);
        AzureEventBus.after("mysql|server.stop", this::onMySqlServerStatusChanged);
        AzureEventBus.after("mysql|server.restart", this::onMySqlServerStatusChanged);
        AzureEventBus.before("mysql|server.start", this::onMySqlServerStatusChanging);
        AzureEventBus.before("mysql|server.stop", this::onMySqlServerStatusChanging);
        AzureEventBus.before("mysql|server.restart", this::onMySqlServerStatusChanging);
        AzureEventBus.before("mysql|server.delete", this::onMySqlServerStatusChanging);
    }

    public void onMySqlServerStatusChanged(MySqlServer server) {
        if (StringUtils.equalsIgnoreCase(this.server.id(), server.id())) {
            this.serverState = server.entity().getState();
        }
    }

    private void onMySqlServerStatusChanging(MySqlServer server) {
        if (StringUtils.equalsIgnoreCase(this.server.id(), server.id())) {
            serverState = SERVER_UPDATING;
        }
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        boolean running = StringUtils.equalsIgnoreCase("READY", serverState);
        boolean updating = StringUtils.equalsIgnoreCase(SERVER_UPDATING, serverState);
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
        boolean running = StringUtils.equalsIgnoreCase("READY", serverState);
        boolean updating = StringUtils.equalsIgnoreCase(SERVER_UPDATING, serverState);

        getNodeActionByName(AzureActionEnum.START.getName()).setEnabled(!updating && !running);
        getNodeActionByName(AzureActionEnum.STOP.getName()).setEnabled(!updating && running);
        getNodeActionByName(AzureActionEnum.RESTART.getName()).setEnabled(!updating && running);
        getNodeActionByName(AzureActionEnum.DELETE.getName()).setEnabled(!updating);
        return super.getNodeActions();
    }

    private void refreshNode() {
        server.refresh();
        this.serverState = server.entity().getState();
    }

    @AzureOperation(name = "mysql.delete", params = {"this.server.name()"}, type = AzureOperation.Type.ACTION)
    private void delete() {
        this.serverState = SERVER_UPDATING;
        this.getParent().removeNode(this.server.entity().getSubscriptionId(), this.getId(), MySQLNode.this);
    }

    @AzureOperation(name = "mysql.start", params = {"this.server.name()"}, type = AzureOperation.Type.ACTION)
    private void start() {
        this.serverState = SERVER_UPDATING;
        this.getServer().start();
        this.refreshNode();
    }

    @AzureOperation(name = "mysql.stop", params = {"this.server.name()"}, type = AzureOperation.Type.ACTION)
    private void stop() {
        this.serverState = SERVER_UPDATING;
        this.getServer().stop();
        this.refreshNode();
    }

    @AzureOperation(name = "mysql.restart", params = {"this.server.name()"}, type = AzureOperation.Type.ACTION)
    private void restart() {
        this.serverState = SERVER_UPDATING;
        this.getServer().restart();
        this.refreshNode();
    }

    @AzureOperation(name = "mysql.open_portal", params = {"this.server.name()"}, type = AzureOperation.Type.ACTION)
    private void openInPortal() {
        this.openResourcesInPortal(this.server.entity().getSubscriptionId(), this.server.id());
    }

    @AzureOperation(name = "mysql.show_properties", params = {"this.server.name()"}, type = AzureOperation.Type.ACTION)
    private void showProperties() {
        DefaultLoader.getUIHelper().openMySQLPropertyView(this.getId(), this.getProject());
    }

    @Override
    public Map<String, String> toProperties() {
        return Collections.singletonMap(TelemetryConstants.SUBSCRIPTIONID, this.server.entity().getSubscriptionId());
    }
}
