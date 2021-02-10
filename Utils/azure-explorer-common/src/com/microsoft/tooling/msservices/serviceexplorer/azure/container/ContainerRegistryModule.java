/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.container;

import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

public class ContainerRegistryModule extends AzureRefreshableNode {

    private static final String ACR_MODULE_ID = ContainerRegistryModule.class.getName();
    private static final String ICON_PATH = "acr.png";
    private static final String BASE_MODULE_NAME = "Container Registries";
    public static final String MODULE_NAME = "Container Registry";
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
    public @Nullable AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.ContainerRegistry.MODULE;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        containerRegistryPresenter.onModuleRefresh();
    }

}
