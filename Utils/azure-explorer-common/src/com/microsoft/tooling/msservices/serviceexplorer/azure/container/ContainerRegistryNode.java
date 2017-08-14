/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.container;

import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.mvp.ui.base.NodeContent;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

import java.util.HashMap;
import java.util.Map;

public class ContainerRegistryNode extends Node implements TelemetryProperties {

    //TODO：change icon
    private static final String ICON_PATH = "RedisCache.png";

    private final String subscriptionId;

    // action name
    private static final String SHOW_PROPERTY_ACTION = "Show properties";

    public ContainerRegistryNode(Node parent, String subscriptionId, NodeContent content) {
        super(subscriptionId + content.getName(), content.getName(),
                parent, ICON_PATH, true /*delayActionLoading*/);
        this.subscriptionId = subscriptionId;
        loadActions();
    }

    @Override
    protected void loadActions() {
        addAction(SHOW_PROPERTY_ACTION, null, new ContainerRegistryNode.ShowContainerRegistryPropertyAction());
        super.loadActions();
    }

    // Show Container Registry property
    public class ShowContainerRegistryPropertyAction extends NodeActionListener {

        @Override
        protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {

        }
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        return properties;
    }
}
