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

import com.microsoft.azure.management.appplatform.v2019_05_01_preview.DeploymentResource;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.ServiceResourceInner;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.DefaultAzureResourceTracker;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.WrappedTelemetryNodeActionListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.OPEN_IN_PORTAL_SPRING_CLOUD_APP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SPRING_CLOUD;
import static com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud.SpringCloudModule.ICON_FILE;

/**
 * SpringCloudNode
 */
public class SpringCloudNode extends RefreshableNode implements TelemetryProperties, SpringCloudNodeView {
    private static final Logger LOGGER = Logger.getLogger(SpringCloudNode.class.getName());
    private static final String FAILED_TO_LOAD_APPS = "Failed to load apps in: %s";
    private static final String ERROR_LOAD_APP = "Azure Services Explorer - Error Loading Spring Cloud Apps";
    private static final String EMPTY_POSTFIX = " (Empty)";
    private static final String ACTION_OPEN_IN_PORTAL = "Open In Portal";

    private final String subscriptionId;
    private String clusterId;
    private String clusterName;
    private SpringCloudNodePresenter springCloudNodePresenter;

    public SpringCloudNode(AzureRefreshableNode parent, String subscriptionId, ServiceResourceInner serviceInner) {
        super(serviceInner.id(), serviceInner.name(), parent, ICON_FILE, true);

        this.subscriptionId = subscriptionId;
        this.clusterId = serviceInner.id();
        this.clusterName = serviceInner.name();
        springCloudNodePresenter = new SpringCloudNodePresenter<>();
        springCloudNodePresenter.onAttachView(this);
        loadActions();
    }

    @Override
    protected void loadActions() {
        super.loadActions();
        addAction(ACTION_OPEN_IN_PORTAL, new WrappedTelemetryNodeActionListener(SPRING_CLOUD, OPEN_IN_PORTAL_SPRING_CLOUD_APP,
                new NodeActionListener() {
                    @Override
                    protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                        openResourcesInPortal(subscriptionId, clusterId);
                    }
                }));
    }

    @Override
    protected void refreshItems() {
        try {
            springCloudNodePresenter.onRefreshSpringCloudServiceNode(this.subscriptionId, this.clusterId);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format(FAILED_TO_LOAD_APPS, this.clusterName), e);
            DefaultLoader.getUIHelper().showException(String.format(FAILED_TO_LOAD_APPS, this.clusterName),
                    e, ERROR_LOAD_APP, false, true);
        }
    }

    @Override
    public void removeAllChildNodes() {
        SpringCloudAppNode[] childNodes = getChildNodes().toArray(new SpringCloudAppNode[0]);
        super.removeAllChildNodes();
        for (SpringCloudAppNode child : childNodes) {
            DefaultAzureResourceTracker.getInstance().unregisterNode(child.getAppId(), child);
        }
    }

    @Override
    public String getIconPath() {
        return ICON_FILE;
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        // todo: track region name
        return properties;
    }

    @Override
    public void removeNode(String sid, String id, Node node) {
        removeDirectChildNode(node);
    }

    @Override
    public void renderSpringCloudApps(List<AppResourceInner> apps, Map<String, DeploymentResource> map) {
        if (apps.isEmpty()) {
            this.setName(this.clusterName + EMPTY_POSTFIX);
        } else {
            this.setName(this.clusterName);
        }
        for (AppResourceInner app : apps) {
            DeploymentResource deploy = map.get(app.name());
            DeploymentResourceInner deploymentResourceInner = deploy != null ? deploy.inner() : null;
            DefaultAzureResourceTracker.getInstance().handleDataChanges(app.id(), app, deploymentResourceInner);
            addChildNode(new SpringCloudAppNode(app, deploymentResourceInner, this));
        }
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getClusterName() {
        return this.clusterName;
    }

    @Override
    public String getServiceName() {
        return TelemetryConstants.SPRING_CLOUD;
    }

    @Override
    public Object getProjectObject() {
        return this.getProject();
    }

}
