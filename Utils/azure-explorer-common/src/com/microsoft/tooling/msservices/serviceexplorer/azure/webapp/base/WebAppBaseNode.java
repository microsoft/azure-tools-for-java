/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base;

import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class WebAppBaseNode extends RefreshableNode implements TelemetryProperties {
    protected static final String ACTION_START = "Start";
    protected static final String ACTION_STOP = "Stop";
    protected static final String ACTION_RESTART = "Restart";
    protected static final String ICON_RUNNING_POSTFIX = "Running_16.png";
    protected static final String ICON_STOPPED_POSTFIX = "Stopped_16.png";

    protected final String label;
    protected final String subscriptionId;
    protected final IAppService appService;
    protected WebAppBaseState state;

    public WebAppBaseNode(final AzureRefreshableNode parent, final String label, final IAppService appService) {
        super(appService.id(), appService.name(), parent, true);
        this.label = label;
        this.appService = appService;
        this.subscriptionId = appService.subscriptionId();

        renderNode(WebAppBaseState.UPDATING);
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            loadActions();
            renderNode(WebAppBaseState.fromString(((IAppService) appService.refresh()).state()));
        });
    }

    protected String getAppServiceIconPath(final WebAppBaseState state) {
        final String os = appService.getRuntime().getOperatingSystem() == OperatingSystem.WINDOWS ? "windows" : "linux";
        return StringUtils.capitalize(os.toLowerCase())
                + label + (state == WebAppBaseState.RUNNING ? ICON_RUNNING_POSTFIX : ICON_STOPPED_POSTFIX);
    }

    @Override
    protected void refreshItems() {
        renderNode(WebAppBaseState.UPDATING);
        appService.refresh();
        renderNode(WebAppBaseState.fromString(appService.state()));
    }

    @Override
    public List<NodeAction> getNodeActions() {
        boolean running = this.state == WebAppBaseState.RUNNING;
        boolean refreshing = this.state == WebAppBaseState.UPDATING;
        getNodeActionByName(ACTION_START).setEnabled(!running && !refreshing);
        getNodeActionByName(ACTION_STOP).setEnabled(running);
        getNodeActionByName(ACTION_RESTART).setEnabled(running);

        return super.getNodeActions();
    }

    public void renderNode(@Nonnull WebAppBaseState state) {
        this.state = state;
        switch (state) {
            case RUNNING:
            case STOPPED:
                this.setIconPath(getAppServiceIconPath(state));
                break;
            case UPDATING:
                this.setIconPath(AzureIconSymbol.Common.REFRESH.getPath());
                break;
            default:
                break;
        }
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        // todo: track region name
        return properties;
    }

    public String getSubscriptionId() {
        return this.subscriptionId;
    }
}
