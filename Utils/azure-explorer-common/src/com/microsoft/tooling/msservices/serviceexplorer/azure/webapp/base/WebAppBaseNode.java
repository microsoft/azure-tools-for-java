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

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

public abstract class WebAppBaseNode extends RefreshableNode implements TelemetryProperties, WebAppBaseNodeView {
    protected static final String ACTION_START = "Start";
    protected static final String ACTION_STOP = "Stop";
    protected static final String ACTION_DELETE = "Delete";
    protected static final String ACTION_RESTART = "Restart";
    protected static final String ACTION_OPEN_IN_BROWSER = "Open In Browser";
    protected static final String ACTION_SHOW_PROPERTY = "Show Properties";
    protected static final String ICON_RUNNING_POSTFIX = "Running_16.png";
    protected static final String ICON_STOPPED_POSTFIX = "Stopped_16.png";
    protected static final String OS_LINUX = "Linux";

    protected final String subscriptionId;
    protected final String hostName;
    protected final String os;
    protected final String label;
    protected WebAppBaseState state;

    public WebAppBaseNode(final String id, final String name, final String label, final AzureRefreshableNode parent,
                          final String subscriptionId, final String hostName, final String os, final String state) {
        super(id, name, parent, getIcon(os, label, WebAppBaseState.fromString(state)), true);
        this.state = WebAppBaseState.fromString(state);
        this.label = label;
        this.subscriptionId = subscriptionId;
        this.os = StringUtils.capitalize(os.toLowerCase());
        this.hostName = hostName;
    }

    protected static String getIcon(final String os, final String label, final WebAppBaseState state) {
        return StringUtils.capitalize(os.toLowerCase())
            + label + (state == WebAppBaseState.RUNNING ? ICON_RUNNING_POSTFIX : ICON_STOPPED_POSTFIX);
    }

    @Override
    public List<NodeAction> getNodeActions() {
        boolean running = this.state == WebAppBaseState.RUNNING;
        getNodeActionByName(ACTION_START).setEnabled(!running);
        getNodeActionByName(ACTION_STOP).setEnabled(running);
        getNodeActionByName(ACTION_RESTART).setEnabled(running);

        return super.getNodeActions();
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

    public String getOs() {
        return this.os;
    }
}
