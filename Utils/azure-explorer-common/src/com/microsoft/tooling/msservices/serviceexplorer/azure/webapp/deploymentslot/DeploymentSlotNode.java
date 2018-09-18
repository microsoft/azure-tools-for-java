package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot;

import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppState;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class DeploymentSlotNode extends Node implements TelemetryProperties {
    private static final String ICON_RUNNING_POSTFIX = "SlotRunning_16.png";
    private static final String ICON_STOPPED_POSTFIX = "SlotStopped_16.png";
    private static final String SLOT_NODE_ID = DeploymentSlotNode.class.getName();
    protected final String subscriptionId;
    protected final String name;
    protected final String os;
    protected DeploymentSlotState state;

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        // todo: track region name
        return properties;
    }

    public DeploymentSlotNode(final DeploymentSlotModule parent, final String name, final String state,
                              final String os, final String subscriptionId) {
        super(SLOT_NODE_ID, name, parent, getIcon(WebAppState.fromString(state), os), true);
        this.subscriptionId = subscriptionId;
        this.os = StringUtils.capitalize(os.toLowerCase());
        this.name = name;
        loadActions();
    }

    protected static String getIcon(final WebAppState state, final String os) {
        return StringUtils.capitalize(os.toLowerCase())
            + (state == WebAppState.RUNNING ? ICON_RUNNING_POSTFIX : ICON_STOPPED_POSTFIX);
    }
}
