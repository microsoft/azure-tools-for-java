/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ServiceResourceInner;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.springcloud.SpringCloudIdHelper;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.util.List;

public class SpringCloudModule extends AzureRefreshableNode implements SpringCloudModuleView {
    protected static final String ICON_FILE = "azure-springcloud-small.png";
    private static final String SPRING_SERVICE_MODULE_ID = SpringCloudModule.class.getName();
    private static final String BASE_MODULE_NAME = "Spring Cloud(Preview)";
    private final SpringCloudModulePresenter<SpringCloudModule> springCloudModulePresenter;

    public static final String MODULE_NAME = "Spring Cloud";

    public SpringCloudModule(final Node parent) {
        super(SPRING_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_FILE);
        springCloudModulePresenter = new SpringCloudModulePresenter<>();
        springCloudModulePresenter.onAttachView(this);
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.SpringCloud.MODULE;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        springCloudModulePresenter.onSpringCloudRefresh();
    }

    @Override
    public void renderChildren(final List<ServiceResourceInner> springCloudServices) {
        for (final ServiceResourceInner resourceEx : springCloudServices) {
            final SpringCloudNode node = new SpringCloudNode(this,
                                                             SpringCloudIdHelper.getSubscriptionId(resourceEx.id()),
                                                             resourceEx);
            addChildNode(node);
        }
    }

    @Override
    public void removeNode(String sid, String id, Node node) {
        ((SpringCloudNode) node).unsubscribe();
        removeDirectChildNode(node);
    }
}
