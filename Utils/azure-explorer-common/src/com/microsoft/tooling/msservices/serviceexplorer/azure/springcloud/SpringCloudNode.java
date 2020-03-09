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
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.ServiceResourceInner;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud.SpringCloudModule.ICON_FILE;

/**
 * SpringCloudNode
 */
public class SpringCloudNode extends RefreshableNode implements TelemetryProperties, SpringCloudNodeView {
    private static final Logger LOGGER = Logger.getLogger(SpringCloudNode.class.getName());
    private static final String FAILED_TO_LOAD_APPS = "Failed to load apps in: %s";
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
    protected void refreshItems() {
        try {
            springCloudNodePresenter.onRefreshSpringCloudServiceNode(this.subscriptionId, this.clusterId);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format(FAILED_TO_LOAD_APPS, this.clusterName), e);
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
    public void renderSpringCloudApps(List<AppResourceInner> apps, Map<String, DeploymentResource> map) {
        if (apps.isEmpty()) {
            this.setName(this.clusterName + " *(Empty)");
        } else {
            this.setName(this.clusterName);
        }
        for (AppResourceInner app : apps) {
            addChildNode(new SpringCloudAppNode(app, map.get(app.name()), this));
        }
    }

    public String getClusterId() {
        return clusterId;
    }

    @Override
    public String getServiceName() {
        return TelemetryConstants.SPRING_CLOUD;
    }
}
