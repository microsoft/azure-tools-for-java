/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.database.postgre;

import com.microsoft.azure.toolkit.ide.common.IExplorerContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.postgre.AzurePostgreSql;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class PostgreSqlExplorerContributor implements IExplorerContributor {
    private static final String NAME = "Azure Database for PostgreSQL";
    private static final String ICON = "/icons/postgre.svg";

    @Override
    public Node<?> getModuleNode() {
        final AzurePostgreSql service = az(AzurePostgreSql.class);
        return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
                .actions(PostgreSqlActionsContributor.SERVICE_ACTIONS)
                .addChildren(this::listPostgreServers, (postgre, serviceNode) -> new Node<>(postgre)
                        .view(new AzureResourceLabelView<>(postgre))
                        .actions(PostgreSqlActionsContributor.POSTGRE_ACTIONS));
    }

    @Nonnull
    private List<PostgreSqlServer> listPostgreServers(AzurePostgreSql s) {
        return s.list().stream().sorted(Comparator.comparing(IAzureBaseResource::name)).collect(Collectors.toList());
    }
}
