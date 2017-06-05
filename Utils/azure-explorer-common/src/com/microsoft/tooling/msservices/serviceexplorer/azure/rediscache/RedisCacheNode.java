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

import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.mvp.ui.base.MvpView;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.HashMap;
import java.util.Map;

public final class RedisCacheNode extends Node implements TelemetryProperties, MvpView {

    private final RedisCache redisCache;
    private final String subscriptionId;
    private final RedisCacheMvpPresenter<RedisCacheModule> redisCachePresenter;

    private static final String DELETE_CONFIRM_DIALOG_FORMAT = "This operation will delete redis cache: %s."
            + "\nAre you sure you want to continue?";
    private static final String DELETE_CONFIRM_TITLE = "Deleting Redis Cache";
    private static final String DELETE_ACTION_NAME = "Delete";
    
    private static final String CREATING_STATE = "Creating";
    private static final String CREATING_REDIS_NAME_FORMAT = "%s(%s...)";
    
    private static final String REDISCACHE_ICON_PATH = "RedisCache.png";

    /**
     * Node for each Redis Cache Resource.
     * @param parent The parent node of this node
     * @param subscriptionId The subscription Id of this Redis Cache
     * @param redisCachePresenter Presenter layer of this View
     * @param redisCache The Redis Cache object of this node
     */
    public RedisCacheNode(Node parent, String subscriptionId, 
            RedisCacheMvpPresenter<RedisCacheModule> redisCachePresenter, RedisCache redisCache) {
        super(subscriptionId + redisCache.name(), redisCache.provisioningState().equals(CREATING_STATE)
                ? String.format(CREATING_REDIS_NAME_FORMAT, redisCache.name(), CREATING_STATE) : redisCache.name(),
                parent, REDISCACHE_ICON_PATH, true);
        this.redisCache = redisCache;
        this.subscriptionId = subscriptionId;
        this.redisCachePresenter = redisCachePresenter;
        loadActions();
    }

    // TODO: add properties action

    public class DeleteRedisCacheAction extends AzureNodeActionPromptListener {
        public DeleteRedisCacheAction() {
            super(RedisCacheNode.this, String.format(DELETE_CONFIRM_DIALOG_FORMAT, redisCache.name()),
                    DELETE_CONFIRM_TITLE);
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e) throws AzureCmdException {
            redisCachePresenter.onRedisCacheDelete(subscriptionId, redisCache.id(), RedisCacheNode.this);
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
        }
    }

    @Override
    protected void loadActions() {
        if (redisCache.provisioningState() != null && !redisCache.provisioningState().equals(CREATING_STATE)) {
            addAction(DELETE_ACTION_NAME, null, new DeleteRedisCacheAction());
        }
        super.loadActions();
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        return properties;
    }

}
