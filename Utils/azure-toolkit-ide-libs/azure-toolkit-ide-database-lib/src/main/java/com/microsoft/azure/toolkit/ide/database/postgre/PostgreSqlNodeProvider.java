/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.database.postgre;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.lib.postgre.AzurePostgreSql;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class PostgreSqlNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Azure Database for PostgreSQL";
    private static final String ICON = "/icons/Microsoft.DBforPostgreSQL/default.svg";

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzurePostgreSql.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent) {
        return data instanceof AzurePostgreSql || data instanceof PostgreSqlServer;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzurePostgreSql) {
            final AzurePostgreSql service = ((AzurePostgreSql) data);
            final Function<AzurePostgreSql, List<PostgreSqlServer>> servers = s -> s.list().stream()
                .flatMap(m -> m.servers().list().stream()).collect(Collectors.toList());
            return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
                .actions(PostgreSqlActionsContributor.SERVICE_ACTIONS)
                .addChildren(servers, (server, serviceNode) -> this.createNode(server, serviceNode, manager));
        } else if (data instanceof PostgreSqlServer) {
            final PostgreSqlServer server = (PostgreSqlServer) data;
            return new Node<>(server)
                .view(new AzureResourceLabelView<>(server))
                .actions(PostgreSqlActionsContributor.SERVER_ACTIONS);
        }
        return null;
    }
}
