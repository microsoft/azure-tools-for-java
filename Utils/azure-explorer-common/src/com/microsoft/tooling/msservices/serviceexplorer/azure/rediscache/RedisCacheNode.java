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

package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.ui.base.NodeContent;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.BasicActionBuilder;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import lombok.Lombok;

import java.util.HashMap;
import java.util.Map;

public class RedisCacheNode extends Node implements TelemetryProperties {

    public static final String TYPE = "Microsoft.Cache/Redis";
    public static final String REDISCACHE_ICON_PATH = "RedisCache.png";

    private final String name;
    private final String resourceId;
    private final String provisionState;
    private final String subscriptionId;

    // node related
    private static final String CREATING_STATE = "Creating";
    private static final String RUNNING_STATE = "Running";
    private static final String SUCCESS_STATE = "Succeeded";
    private static final String STOPPED_STATE = "Stopped";
    private static final String CREATING_REDIS_NAME_FORMAT = "%s(%s...)";

    // action names
    private static final String OPEN_EXPLORER = "Open Redis Explorer";

    // string format
    private static final String DELETE_CONFIRM_DIALOG_FORMAT = "This operation will delete redis cache: %s." +
            "\nAre you sure you want to continue?";
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
        super(subscriptionId + content.getName(), content.getProvisionState().equals(CREATING_STATE) ?
                String.format(CREATING_REDIS_NAME_FORMAT, content.getName(), CREATING_STATE)
                : content.getName(), parent, REDISCACHE_ICON_PATH, true);
        this.name = content.getName();
        this.resourceId = content.getId();
        this.provisionState = content.getProvisionState();
        this.subscriptionId = subscriptionId;
        loadActions();
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        boolean running = RUNNING_STATE.equalsIgnoreCase(provisionState) || SUCCESS_STATE.equalsIgnoreCase(provisionState);
        boolean stopped = STOPPED_STATE.equalsIgnoreCase(provisionState);
        return running ? AzureIconSymbol.RedisCache.RUNNING : !stopped ? AzureIconSymbol.RedisCache.UPDATING : AzureIconSymbol.RedisCache.STOPPED;
    }

    @Override
    protected void loadActions() {
        if (!CREATING_STATE.equals(this.provisionState)) {
            addAction(initActionBuilder(this::delete).withAction(AzureActionEnum.DELETE).withBackgroudable(true).withPromptable(true).build());
            addAction(initActionBuilder(this::showProperties).withAction(AzureActionEnum.SHOW_PROPERTIES).build());
            addAction(OPEN_EXPLORER, initActionBuilder(this::openExplorer).build("Opening"));
        }
        addAction(initActionBuilder(this::openInPortal).withAction(AzureActionEnum.OPEN_IN_PORTAL).withBackgroudable(true).build());
        super.loadActions();
    }

    private BasicActionBuilder initActionBuilder(Runnable runnable) {
        return new BasicActionBuilder(runnable)
                .withModuleName(RedisCacheModule.MODULE_NAME)
                .withInstanceName(name);
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

    @AzureOperation(name = ActionConstants.RedisCache.DELETE, type = AzureOperation.Type.ACTION)
    private void delete() {
        this.getParent().removeNode(this.subscriptionId, this.resourceId, this);
    }

    @AzureOperation(name = ActionConstants.RedisCache.OPEN_IN_PORTAL, type = AzureOperation.Type.ACTION)
    private void openInPortal() {
        String portalUrl = "";
        try {
            portalUrl = AuthMethodManager.getInstance().getAzureManager().getPortalUrl();
        } catch (Exception exception) {
            Lombok.sneakyThrow(exception);
        }
        DefaultLoader.getUIHelper().openInBrowser(String.format(AZURE_PORTAL_LINK_FORMAT, portalUrl, this.resourceId));
    }

    private void showProperties() {
        DefaultLoader.getUIHelper().openRedisPropertyView(this);
    }

    private void openExplorer() {
        DefaultLoader.getUIHelper().openRedisExplorer(this);
    }

}
