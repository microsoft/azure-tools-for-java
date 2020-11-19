/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base;

import com.microsoft.azure.CommonIcons;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.*;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class WebAppBaseNode extends RefreshableNode implements TelemetryProperties, WebAppBaseNodeView {
    protected static final String ACTION_START = "Start";
    protected static final String ACTION_STOP = "Stop";
    protected static final String ACTION_DELETE = "Delete";
    protected static final String ACTION_RESTART = "Restart";
    protected static final String ACTION_OPEN_IN_BROWSER = "Open In Browser";
    protected static final String ACTION_SHOW_PROPERTY = "Show Properties";
    protected static final String ICON_RUNNING_POSTFIX = "Running.svg";
    protected static final String ICON_STOPPED_POSTFIX = "Stopped.svg";

    protected final String subscriptionId;
    protected final String hostName;
    protected final String os;
    protected final String label;
    protected WebAppBaseState state;

    private NodeAction startAction;
    private NodeAction restartAction;
    private NodeAction stopAction;
    private NodeAction startStreamingLogsAction;
    private NodeAction stopStreamingLogsAction;

    public WebAppBaseNode(final String id, final String name, final String label, final AzureRefreshableNode parent,
                          final String subscriptionId, final String hostName, final String os, final String state) {
        super(id, name, parent, null, true);
        this.iconPath = getIcon(os, label, WebAppBaseState.fromString(state));
        this.state = WebAppBaseState.fromString(state);
        this.label = label;
        this.subscriptionId = subscriptionId;
        this.os = StringUtils.capitalize(os.toLowerCase());
        this.hostName = hostName;
    }

    protected abstract NodeActionListener getStartActionListener();
    protected abstract NodeActionListener getRestartActionListener();
    protected abstract NodeActionListener getStopActionListener();
    protected abstract NodeActionListener getShowPropertiesActionListener();
    protected abstract NodeActionListener getOpenInBrowserActionListener();
    protected abstract NodeActionListener getDeleteActionListener();

    @Override
    protected void loadActions() {
        addAction(ACTION_START, CommonIcons.ACTION_START, getStartActionListener());
        addAction(ACTION_RESTART, CommonIcons.ACTION_RESTART, getRestartActionListener());
        addAction(ACTION_STOP, CommonIcons.ACTION_STOP, getStopActionListener());
        addAction(ACTION_SHOW_PROPERTY, CommonIcons.ACTION_OPEN_PREFERENCES, getShowPropertiesActionListener());
        addAction(ACTION_OPEN_IN_BROWSER, CommonIcons.ACTION_OPEN_IN_BROWSER, getOpenInBrowserActionListener());
        addAction(ACTION_DELETE, CommonIcons.ACTION_DISCARD, getDeleteActionListener(), NodeActionPosition.BOTTOM);

        super.loadActions();
    }

    // Remove static to provide ability to override IconPath logic for [FunctionAppNode].
    protected String getIcon(final String os, final String label, final WebAppBaseState state) {
        return StringUtils.capitalize(os.toLowerCase())
            + label + (state == WebAppBaseState.RUNNING ? ICON_RUNNING_POSTFIX : ICON_STOPPED_POSTFIX);
    }

    @Override
    public List<NodeAction> getNodeActions() {
        boolean running = this.state == WebAppBaseState.RUNNING;

        // Show/hide Azure Start/Stop/Restart actions based on app state.
        if (startAction == null)
            startAction = getNodeActionByName(ACTION_START);

        if (restartAction == null)
            restartAction = getNodeActionByName(ACTION_RESTART);

        if (stopAction == null)
            stopAction = getNodeActionByName(ACTION_STOP);

        List<NodeAction> nodeActions = super.getNodeActions();

        if (running) {
            nodeActions.remove(startAction);
            if (!nodeActions.contains(stopAction))
                nodeActions.add(stopAction);
        } else {
            nodeActions.remove(stopAction);
            if (!nodeActions.contains(startAction))
                nodeActions.add(startAction);
        }

        restartAction.setEnabled(running);

        // Hack for Azure Streaming Logs frontend actions to hide unnecessary actions
        if (startStreamingLogsAction == null)
            startStreamingLogsAction = getNodeActionByName("Start Streaming Logs");

        if (stopStreamingLogsAction == null)
            stopStreamingLogsAction = getNodeActionByName("Stop Streaming Logs");

        if (isStreamingLogStarted()) {
            nodeActions.remove(startStreamingLogsAction);
            if (!nodeActions.contains(stopStreamingLogsAction))
                nodeActions.add(stopStreamingLogsAction);

        } else {
            nodeActions.remove(stopStreamingLogsAction);
            if (!nodeActions.contains(startStreamingLogsAction))
                nodeActions.add(startStreamingLogsAction);
        }

        return nodeActions;
    }

    protected NodeActionListener createBackgroundActionListener(final String actionName, final Runnable runnable) {
        return new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getIdeHelper().runInBackground(null, actionName, false,
                    true, String.format("%s...", actionName), runnable);
            }
        };
    }

    @Override
    public void renderNode(@NotNull WebAppBaseState state) {
        switch (state) {
            case RUNNING:
                this.state = state;
                this.setIconPath(getIcon(this.os, this.label, WebAppBaseState.RUNNING));
                break;
            case STOPPED:
                this.state = state;
                this.setIconPath(getIcon(this.os, this.label, WebAppBaseState.STOPPED));
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

    public boolean isStreamingLogStarted() {
        return WebAppBaseStreamingLogs.INSTANCE.isStreamingLogsStarted(this.id);
    }

    public void setStreamingLogStarted(boolean value) {
        WebAppBaseStreamingLogs.INSTANCE.setStreamingLogsStarted(this.id, value);
    }

    public String getOs() {
        return this.os;
    }
}
