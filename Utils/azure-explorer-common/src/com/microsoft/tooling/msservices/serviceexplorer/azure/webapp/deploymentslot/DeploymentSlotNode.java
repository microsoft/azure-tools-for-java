package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot;

import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import java.util.HashMap;
import java.util.Map;

public class DeploymentSlotNode extends Node implements TelemetryProperties {
    private static final String ICON_RUNNING = "SlotRunning_16.png";
    private static final String ICON_STOPPED = "SlotStopped_16.png";
    private static final String SLOT_NODE_ID = DeploymentSlotNode.class.getName();
    protected final String subscriptionId;
    protected final String name;
    protected DeploymentSlotState state;

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        // todo: track region name
        return properties;
    }

    public DeploymentSlotNode(final DeploymentSlotModule parent, final String name, final String state, final String subscriptionId) {
        super(SLOT_NODE_ID,
            name,
            parent,
            DeploymentSlotState.fromString(state) == DeploymentSlotState.RUNNING ? ICON_RUNNING : ICON_STOPPED,
            true);
        this.subscriptionId = subscriptionId;
        this.name = name;
        loadActions();
    }
}
