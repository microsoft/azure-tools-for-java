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

package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.DELETE_REDIS;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.REDIS;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.REDIS_OPEN_BROWSER;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.REDIS_OPEN_EXPLORER;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.REDIS_READPROP;

import com.microsoft.tooling.msservices.serviceexplorer.WrappedTelemetryNodeActionListener;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.mvp.ui.base.NodeContent;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

public class RedisCacheNode extends Node implements TelemetryProperties {

    public static final String TYPE = "Microsoft.Cache/Redis";
    public static final String REDISCACHE_ICON_PATH = "RedisCache.png";

    private final String name;
    private final String resourceId;
    private final String provisionState;
    private final String subscriptionId;

    // node related
    private static final String CREATING_STATE = "Creating";
    private static final String CREATING_REDIS_NAME_FORMAT = "%s(%s...)";

    // action names
    private static final String DELETE_ACTION = "Delete";
    private static final String SHOW_PROPERTY_ACTION = "Show Properties";
    private static final String OPEN_IN_BROWSER_ACTION = "Open in Browser";
    private static final String OPEN_EXPLORER = "Open Redis Explorer";

    // string format
    private static final String DELETE_CONFIRM_DIALOG_FORMAT = "This operation will delete redis cache: %s."
            + "\nAre you sure you want to continue?";
    private static final String DELETE_CONFIRM_TITLE = "Deleting Redis Cache";
    private static final String AZURE_PORTAL_LINK_FORMAT = "%s/#resource/%s/overview";

    /**
     * Node for each Redis Cache Resource.
     *
     * @param parent
     *            The parent node of this node
     * @param subscriptionId
     *            The subscription Id of this Redis Cache
     * @param content
     *            The basic information object for the node
     */
    public RedisCacheNode(Node parent, String subscriptionId, NodeContent content) {
        super(subscriptionId + content.getName(), content.getProvisionState().equals(CREATING_STATE)
                ? String.format(CREATING_REDIS_NAME_FORMAT, content.getName(), CREATING_STATE)
                : content.getName(), parent, REDISCACHE_ICON_PATH, true);
        this.name = content.getName();
        this.resourceId = content.getId();
        this.provisionState = content.getProvisionState();
        this.subscriptionId = subscriptionId;
        loadActions();
    }

    // Show property action class
    private class ShowRedisCachePropertyAction extends NodeActionListener {

        @Override
        protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
            DefaultLoader.getUIHelper().openRedisPropertyView(RedisCacheNode.this);
        }
    }

    // Delete Redis Cache action class
    private class DeleteRedisCacheAction extends AzureNodeActionPromptListener {
        public DeleteRedisCacheAction() {
            super(RedisCacheNode.this, String.format(DELETE_CONFIRM_DIALOG_FORMAT, RedisCacheNode.this.name),
                    DELETE_CONFIRM_TITLE);
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e) throws AzureCmdException {
            RedisCacheNode.this.getParent().removeNode(RedisCacheNode.this.subscriptionId,
                RedisCacheNode.this.resourceId, RedisCacheNode.this);
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
        }

        @Override
        protected String getServiceName() {
            return REDIS;
        }

        @Override
        protected String getOperationName(NodeActionEvent event) {
            return DELETE_REDIS;
        }
    }

    // Open in browser action class
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
                    RedisCacheNode.this.resourceId));
        }
    }

    // Open Redis Cache Explorer
    private class OpenRedisExplorerAction extends NodeActionListener {
        @Override
        protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
            DefaultLoader.getUIHelper().openRedisExplorer(RedisCacheNode.this);
        }
    }

    @Override
    protected void loadActions() {
        if (!CREATING_STATE.equals(this.provisionState)) {
            addAction(DELETE_ACTION, null, new WrappedTelemetryNodeActionListener(REDIS, DELETE_REDIS,
                new DeleteRedisCacheAction()));
            addAction(SHOW_PROPERTY_ACTION, null, new WrappedTelemetryNodeActionListener(REDIS, REDIS_READPROP,
                new ShowRedisCachePropertyAction()));
            addAction(OPEN_EXPLORER, null, new WrappedTelemetryNodeActionListener(REDIS, REDIS_OPEN_EXPLORER,
                new OpenRedisExplorerAction()));
        }
        addAction(OPEN_IN_BROWSER_ACTION, null, new WrappedTelemetryNodeActionListener(REDIS, REDIS_OPEN_BROWSER,
            new OpenInBrowserAction()));
        super.loadActions();
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
