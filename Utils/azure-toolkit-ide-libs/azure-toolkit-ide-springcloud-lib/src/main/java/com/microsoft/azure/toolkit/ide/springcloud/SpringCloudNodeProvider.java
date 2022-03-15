/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.springcloud;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class SpringCloudNodeProvider implements IExplorerNodeProvider {

    private static final String NAME = "Spring Cloud";
    private static final String ICON = "/icons/Microsoft.AppPlatform/default.svg";

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzureSpringCloud.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent) {
        return data instanceof AzureSpringCloud ||
            data instanceof SpringCloudCluster ||
            data instanceof SpringCloudApp;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureSpringCloud) {
            final AzureSpringCloud service = (AzureSpringCloud) data;
            final Function<AzureSpringCloud, List<SpringCloudCluster>> clusters = asc -> asc.list().stream().flatMap(m -> m.clusters().list().stream())
                .collect(Collectors.toList());
            return new Node<>(service).view(new AzureServiceLabelView<>(service, "Spring Cloud", ICON))
                .actions(SpringCloudActionsContributor.SERVICE_ACTIONS)
                .addChildren(clusters, (cluster, ascNode) -> this.createNode(cluster, ascNode, manager));
        } else if (data instanceof SpringCloudCluster) {
            final SpringCloudCluster cluster = (SpringCloudCluster) data;
            return new Node<>(cluster)
                .view(new AzureResourceLabelView<>(cluster))
                .inlineAction(ResourceCommonActionsContributor.PIN)
                .actions(SpringCloudActionsContributor.CLUSTER_ACTIONS)
                .addChildren(c -> c.apps().list(), (app, clusterNode) -> this.createNode(app, clusterNode, manager));
        } else if (data instanceof SpringCloudApp) {
            final SpringCloudApp app = (SpringCloudApp) data;
            return new Node<>(app)
                .view(new AzureResourceLabelView<>(app))
                .inlineAction(ResourceCommonActionsContributor.PIN)
                .actions(SpringCloudActionsContributor.APP_ACTIONS);
        }
        return null;
    }
}
