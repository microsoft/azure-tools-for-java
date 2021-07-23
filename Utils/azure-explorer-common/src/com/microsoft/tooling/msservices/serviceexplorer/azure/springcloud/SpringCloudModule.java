/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

public class SpringCloudModule extends AzureRefreshableNode {
    private static final String SPRING_SERVICE_MODULE_ID = SpringCloudModule.class.getName();
    private static final String BASE_MODULE_NAME = "Spring Cloud";

    public static final String MODULE_NAME = "Spring Cloud";

    public SpringCloudModule(final Node parent) {
        super(SPRING_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, null);
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.SpringCloud.MODULE;
    }

    @Override
    @AzureOperation(name = "springcloud|cluster.list.subscription|selected", type = AzureOperation.Type.ACTION)
    protected void refreshItems() throws AzureCmdException {
        Azure.az(AzureSpringCloud.class).clusters(true).forEach(cluster -> {
            final SpringCloudNode node = new SpringCloudNode(this, cluster);
            addChildNode(node);
        });
    }

    @Override
    public void removeNode(String sid, String id, Node node) {
        ((SpringCloudNode) node).unsubscribe();
        removeDirectChildNode(node);
    }
}
