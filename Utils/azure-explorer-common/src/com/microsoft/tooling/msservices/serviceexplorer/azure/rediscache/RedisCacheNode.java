/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.ui.base.NodeContent;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
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
    public @Nullable AzureIcon getIconSymbol() {
        boolean running = RUNNING_STATE.equalsIgnoreCase(provisionState) || SUCCESS_STATE.equalsIgnoreCase(provisionState);
        boolean stopped = STOPPED_STATE.equalsIgnoreCase(provisionState);
        return running ? AzureIcons.RedisCache.RUNNING : !stopped ? AzureIcons.RedisCache.UPDATING : AzureIcons.RedisCache.STOPPED;
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

    @AzureOperation(name = "redis.delete_redis.redis", params = {"this.name"}, type = AzureOperation.Type.ACTION)
    private void delete() {
        this.getParent().removeNode(this.subscriptionId, this.resourceId, this);
    }

    @AzureOperation(name = "redis.open_portal.redis", params = {"this.name"}, type = AzureOperation.Type.ACTION)
    private void openInPortal() {
        String portalUrl = "";
        try {
            portalUrl = Azure.az(AzureAccount.class).account().getPortalUrl();
        } catch (Exception exception) {
            Lombok.sneakyThrow(exception);
        }
        DefaultLoader.getUIHelper().openInBrowser(String.format(AZURE_PORTAL_LINK_FORMAT, portalUrl, this.resourceId));
    }

    @AzureOperation(name = "redis.show_properties.redis", params = {"this.name"}, type = AzureOperation.Type.ACTION)
    private void showProperties() {
        DefaultLoader.getUIHelper().openRedisPropertyView(this);
    }

    @AzureOperation(name = "redis.open_redis_explorer.redis", params = {"this.name"}, type = AzureOperation.Type.ACTION)
    private void openExplorer() {
        DefaultLoader.getUIHelper().openRedisExplorer(this);
    }

}
