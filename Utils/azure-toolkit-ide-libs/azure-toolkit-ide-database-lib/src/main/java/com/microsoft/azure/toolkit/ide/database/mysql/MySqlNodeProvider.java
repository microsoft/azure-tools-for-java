/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.database.mysql;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySql;
import com.microsoft.azure.toolkit.lib.mysql.MySqlServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class MySqlNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Azure Database for MySQL";
    private static final String ICON = "/icons/Microsoft.DBforMySQL/default.svg";

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzureMySql.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent) {
        return data instanceof AzureMySql || data instanceof MySqlServer;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureMySql) {
            final AzureMySql service = ((AzureMySql) data);
            final Function<AzureMySql, List<MySqlServer>> servers = s -> s.list().stream()
                .flatMap(m -> m.servers().list().stream()).collect(Collectors.toList());
            return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
                .actions(MySqlActionsContributor.SERVICE_ACTIONS)
                .addChildren(servers, (server, serviceNode) -> this.createNode(server, serviceNode, manager));
        } else if (data instanceof MySqlServer) {
            final MySqlServer server = (MySqlServer) data;
            return new Node<>(server)
                .view(new AzureResourceLabelView<>(server))
                .inlineAction(ResourceCommonActionsContributor.PIN)
                .actions(MySqlActionsContributor.SERVER_ACTIONS);
        }
        return null;
    }

}
