/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.sqlserver;

import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.sqlserver.SqlServer;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.BasicActionBuilder;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SqlServerNode extends Node implements TelemetryProperties {
    private static final String SERVER_READY = "Ready";
    private static final String SERVER_UPDATING = "Updating";
    @Getter
    private final String subscriptionId;
    @Getter
    private final SqlServer server;
    private String serverState;

    public SqlServerNode(AzureRefreshableNode parent, String subscriptionId, SqlServer server) {
        super(server.entity().getId(), server.entity().getName(), parent, true);
        this.subscriptionId = subscriptionId;
        this.server = server;
        this.loadActions();
        AzureEventBus.before("sqlserver|server.delete", this::onServerStatusChanging);
    }

    private void onServerStatusChanging(SqlServer server) {
        if (StringUtils.equalsIgnoreCase(this.server.entity().getId(), server.entity().getId())) {
            serverState = SERVER_UPDATING;
        }
    }

    @Override
    @Nonnull
    public AzureIconSymbol getIconSymbol() {
        boolean running = SERVER_READY.equals(server.entity().getState());
        boolean updating = SERVER_UPDATING.equals(serverState);
        return updating ? AzureIconSymbol.SqlServer.UPDATING : (running ? AzureIconSymbol.SqlServer.RUNNING : AzureIconSymbol.SqlServer.STOPPED);
    }

    @Override
    protected void loadActions() {
        addAction(initActionBuilder(this::delete).withAction(AzureActionEnum.DELETE).withBackgroudable(true).withPromptable(true).build());
        addAction(initActionBuilder(this::openInPortal).withAction(AzureActionEnum.OPEN_IN_PORTAL).withBackgroudable(true).build());
        addAction(initActionBuilder(this::showProperties).withAction(AzureActionEnum.SHOW_PROPERTIES).build());
        this.initActions();
    }

    private BasicActionBuilder initActionBuilder(Runnable runnable) {
        return (new BasicActionBuilder(runnable)).withModuleName("Sql Server").withInstanceName(this.name);
    }

    @Override
    public List<NodeAction> getNodeActions() {
        final boolean updating = SERVER_UPDATING.equals(serverState);
        this.getNodeActionByName(AzureActionEnum.DELETE.getName()).setEnabled(!updating);
        return super.getNodeActions();
    }

    private void delete() {
        this.getParent().removeNode(this.subscriptionId, this.getId(), SqlServerNode.this);
    }

    @AzureOperation(name = "sqlserver|server.open_portal", params = {"this.server.entity().getName()"}, type = AzureOperation.Type.ACTION)
    private void openInPortal() {
        this.openResourcesInPortal(this.subscriptionId, this.server.entity().getId());
    }

    @AzureOperation(name = "sqlserver|server.show_properties", params = {"this.server.entity().getName()"}, type = AzureOperation.Type.ACTION)
    private void showProperties() {
        DefaultLoader.getUIHelper().openSqlServerPropertyView(this.getId(), this.getProject());
    }

    @Override
    public Map<String, String> toProperties() {
        return Collections.singletonMap("subscriptionId", this.subscriptionId);
    }

}
