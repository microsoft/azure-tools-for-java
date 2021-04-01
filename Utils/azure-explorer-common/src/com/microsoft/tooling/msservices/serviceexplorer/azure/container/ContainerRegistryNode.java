/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2018-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.container;

import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.BasicActionBuilder;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.util.HashMap;
import java.util.Map;

public class ContainerRegistryNode extends Node implements TelemetryProperties {

    public static final String ICON_PATH = "acr.svg";

    private final String subscriptionId;
    private final String resourceId;

    // string formatter
    private static final String AZURE_PORTAL_LINK_FORMAT = "%s/#resource/%s/overview";

    /**
     * Constructor of the node for an ACR resource.
     */
    public ContainerRegistryNode(ContainerRegistryModule parent, String subscriptionId, String registryId, String
            registryName) {
        super(subscriptionId + registryName, registryName,
                parent, null, true /*delayActionLoading*/);
        this.subscriptionId = subscriptionId;
        this.resourceId = registryId;
        loadActions();
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.ContainerRegistry.MODULE;
    }

    @Override
    protected void loadActions() {
        addAction(initActionBuilder(this::openInPortal).withAction(AzureActionEnum.OPEN_IN_PORTAL).withBackgroudable(true).build());
        addAction(initActionBuilder(this::showProperties).withAction(AzureActionEnum.SHOW_PROPERTIES).build());
        super.loadActions();
    }

    protected final BasicActionBuilder initActionBuilder(Runnable runnable) {
        return new BasicActionBuilder(runnable)
                .withModuleName(ContainerRegistryModule.MODULE_NAME)
                .withInstanceName(name);
    }

    @AzureOperation(name = ActionConstants.ContainerRegister.SHOW_PROPERTIES, type = AzureOperation.Type.ACTION)
    private void showProperties() {
        DefaultLoader.getUIHelper().openContainerRegistryPropertyView(ContainerRegistryNode.this);
    }

    @AzureOperation(name = ActionConstants.ContainerRegister.OPEN_IN_PORTAL, type = AzureOperation.Type.ACTION)
    private void openInPortal() {
        String portalUrl = AuthMethodManager.getInstance().getAzureManager().getPortalUrl();
        DefaultLoader.getUIHelper().openInBrowser(String.format(AZURE_PORTAL_LINK_FORMAT, portalUrl,
                ContainerRegistryNode.this.resourceId));
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        return properties;
    }

    public String getSubscriptionId() {
        return this.subscriptionId;
    }

    public String getResourceId() {
        return this.resourceId;
    }
}
