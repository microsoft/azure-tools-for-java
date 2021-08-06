/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.springcloud;

import com.microsoft.azure.toolkit.ide.common.IExplorerContributor;
import com.microsoft.azure.toolkit.ide.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.IView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class SpringCloudExplorerContributor implements IExplorerContributor {

    private static final String NAME = "Spring Cloud";
    private static final String ICON = "/icons/springcloud.svg";

    @Override
    public Node<?> getNode() {
        final AzureActionManager am = AzureActionManager.getInstance();
        final IAzureMessager messager = AzureMessager.getDefaultMessager();

        return new Node<>(az(AzureSpringCloud.class)).view(new IView.Label.Static(NAME, ICON))
                .addChildren(AzureSpringCloud::clusters, (cluster, ascNode) -> new Node<>(cluster)
                        .view(new AzureResourceLabelView<>(cluster))
                        .actions(SpringCloudActions.CLUSTER_ACTIONS)
                        .addChildren(SpringCloudCluster::apps, (app, clusterNode) -> new Node<>(app)
                                .view(new AzureResourceLabelView<>(app))
                                .actions(SpringCloudActions.APP_ACTIONS)));
    }
}
