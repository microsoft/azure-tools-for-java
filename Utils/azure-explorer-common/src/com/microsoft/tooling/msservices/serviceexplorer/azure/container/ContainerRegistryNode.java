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

import static com.microsoft.azuretools.telemetry.TelemetryConstants.ACR;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.ACR_OPEN_EXPLORER;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.ACR_OPEN_INBROWSER;

import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

import com.microsoft.tooling.msservices.serviceexplorer.WrappedTelemetryNodeActionListener;
import java.util.HashMap;
import java.util.Map;

public class ContainerRegistryNode extends Node implements TelemetryProperties {

    public static final String ICON_PATH = "acr.png";

    private final String subscriptionId;
    private final String resourceId;

    // action name
    private static final String OPEN_EXPLORER_ACTION = "Open ACR Explorer";
    private static final String OPEN_IN_BROWSER_ACTION = "Open in browser";

    // string formatter
    private static final String AZURE_PORTAL_LINK_FORMAT = "%s/#resource/%s/overview";

    /**
     * Constructor of the node for an ACR resource.
     */
    public ContainerRegistryNode(ContainerRegistryModule parent, String subscriptionId, String registryId, String
            registryName) {
        super(subscriptionId + registryName, registryName,
                parent, ICON_PATH, true /*delayActionLoading*/);
        this.subscriptionId = subscriptionId;
        this.resourceId = registryId;
        loadActions();
    }

    @Override
    protected void loadActions() {
        addAction(OPEN_EXPLORER_ACTION, null, new WrappedTelemetryNodeActionListener(ACR, ACR_OPEN_EXPLORER,
            new ShowContainerRegistryPropertyAction()));
        addAction(OPEN_IN_BROWSER_ACTION, null, new WrappedTelemetryNodeActionListener(ACR, ACR_OPEN_INBROWSER,
            new OpenInBrowserAction()));
        super.loadActions();
    }

    // Show Container Registry property
    private class ShowContainerRegistryPropertyAction extends NodeActionListener {

        @Override
        protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
            DefaultLoader.getUIHelper().openContainerRegistryPropertyView(ContainerRegistryNode.this);
        }
    }

    // Open in browser action
    private class OpenInBrowserAction extends NodeActionListener {

        @Override
        protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
            String portalUrl = "";
            try {
                portalUrl = AuthMethodManager.getInstance().getAzureManager().getPortalUrl();
            } catch (Exception exception) {
                System.out.println(exception.getMessage());
            }
            DefaultLoader.getUIHelper().openInBrowser(String.format(AZURE_PORTAL_LINK_FORMAT, portalUrl,
                    ContainerRegistryNode.this.resourceId));
        }

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
