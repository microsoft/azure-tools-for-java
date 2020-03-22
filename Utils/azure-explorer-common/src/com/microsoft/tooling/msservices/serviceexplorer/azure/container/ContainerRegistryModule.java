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
package com.microsoft.tooling.msservices.serviceexplorer.azure.container;

import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

public class ContainerRegistryModule extends AzureRefreshableNode {

    private static final String ACR_MODULE_ID = ContainerRegistryModule.class.getName();
    private static final String ICON_PATH = "acr.png";
    private static final String BASE_MODULE_NAME = "Container Registries";
    private final ContainerRegistryModulePresenter<ContainerRegistryModule> containerRegistryPresenter;

    /**
     * The root node for ACR resource.
     */
    public ContainerRegistryModule(Node parent) {
        super(ACR_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
        containerRegistryPresenter = new ContainerRegistryModulePresenter<>();
        containerRegistryPresenter.onAttachView(ContainerRegistryModule.this);
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        containerRegistryPresenter.onModuleRefresh();
    }

}
