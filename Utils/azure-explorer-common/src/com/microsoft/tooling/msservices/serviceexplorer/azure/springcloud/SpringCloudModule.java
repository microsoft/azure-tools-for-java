/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.ServiceResourceInner;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.mvp.model.springcloud.SpringCloudIdHelper;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.DefaultAzureResourceTracker;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.util.List;

public class SpringCloudModule extends AzureRefreshableNode implements SpringCloudModuleView {
    protected static final String ICON_FILE = "azure-springcloud-small.png";
    private static final String SPRING_SERVICE_MODULE_ID = SpringCloudModule.class.getName();
    private static final String BASE_MODULE_NAME = "Spring Cloud(Preview)";
    private final SpringCloudModulePresenter<SpringCloudModule> springCloudModulePresenter;

    public SpringCloudModule(final Node parent) {
        super(SPRING_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_FILE);
        springCloudModulePresenter = new SpringCloudModulePresenter<>();
        springCloudModulePresenter.onAttachView(this);
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
        SpringCloudNode springNode = (SpringCloudNode) node;
        DefaultAzureResourceTracker.getInstance().registerNode(springNode.getClusterId(), springNode);
        removeDirectChildNode(node);
    }

}
