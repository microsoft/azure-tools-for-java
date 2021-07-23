/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.BasicActionBuilder;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SpringCloudNode
 */
public class SpringCloudNode extends RefreshableNode implements TelemetryProperties {
    private static final String EMPTY_POSTFIX = " (Empty)";

    @Getter
    private final SpringCloudCluster cluster;

    public SpringCloudNode(AzureRefreshableNode parent, SpringCloudCluster cluster) {
        super(cluster.id(), cluster.name(), parent, null, true);
        this.cluster = cluster;
        loadActions();
        AzureEventBus.after("springcloud|app.create", this::onAppCreatedOrRemoved);
        AzureEventBus.after("springcloud|app.remove", this::onAppCreatedOrRemoved);
    }

    public void onAppCreatedOrRemoved(SpringCloudApp app) {
        if (this.cluster.name().equals(app.getCluster().name())) {
            this.load(true);
        }
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.SpringCloud.CLUSTER;
    }

    @Override
    protected void loadActions() {
        super.loadActions();
        addAction(initActionBuilder(this::openInPortal).withAction(AzureActionEnum.OPEN_IN_PORTAL).withBackgroudable(true).build());
    }

    private BasicActionBuilder initActionBuilder(Runnable runnable) {
        return new BasicActionBuilder(runnable)
                .withModuleName(SpringCloudModule.MODULE_NAME)
                .withInstanceName(name);
    }

    @Override
    @AzureOperation(name = "springcloud|app.list.cluster", params = "this.cluster.name()", type = AzureOperation.Type.ACTION)
    protected void refreshItems() {
        final List<SpringCloudApp> apps = cluster.refresh().apps();
        this.setName(CollectionUtils.isEmpty(apps) ? this.cluster.name() + EMPTY_POSTFIX : this.cluster.name());
        apps.forEach(app -> addChildNode(new SpringCloudAppNode(app, this)));
    }

    @Override
    public void removeAllChildNodes() {
        final SpringCloudAppNode[] childNodes = getChildNodes().toArray(new SpringCloudAppNode[0]);
        super.removeAllChildNodes();
        for (final SpringCloudAppNode child : childNodes) {
            child.unsubscribe();
        }
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.cluster.entity().getSubscriptionId());
        // todo: track region name
        return properties;
    }

    @Override
    public void removeNode(String sid, String id, Node node) {
        ((SpringCloudAppNode) node).unsubscribe();
        removeDirectChildNode(node);
    }

    @Override
    public String getServiceName() {
        return TelemetryConstants.SPRING_CLOUD;
    }

    public void unsubscribe() {
        // TODO: remove cluster event listeners
    }

    private void openInPortal() {
        this.openResourcesInPortal(this.cluster.entity().getSubscriptionId(), this.cluster.id());
    }

}
