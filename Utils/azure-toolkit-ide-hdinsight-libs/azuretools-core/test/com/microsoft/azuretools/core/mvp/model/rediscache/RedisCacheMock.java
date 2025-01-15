/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.rediscache;

import java.util.List;
import java.util.Map;

import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.PagedIterable;
import com.azure.resourcemanager.redis.models.*;
import com.azure.resourcemanager.redis.RedisManager;
import com.azure.core.management.Region;
import com.azure.resourcemanager.redis.fluent.models.RedisResourceInner;

import com.azure.resourcemanager.resources.fluentcore.arm.models.PrivateEndpointConnection;
import com.azure.resourcemanager.resources.fluentcore.arm.models.PrivateLinkResource;
import reactor.core.publisher.Mono;

public class RedisCacheMock implements RedisCache{

    private static final String MOCK_STRING = "test";

    public String hostName() {
        return MOCK_STRING;
    }

    public RedisAccessKeys keys() {
        return new RedisAccessKeysMock();
    }

    public int sslPort() {
        return 0;
    }

    @Override
    public Region region() {
        return null;
    }

    @Override
    public String regionName() {
        return MOCK_STRING;
    }

    @Override
    public Map<String, String> tags() {
        return null;
    }

    @Override
    public String type() {
        return MOCK_STRING;
    }

    @Override
    public String key() {
        return MOCK_STRING;
    }

    @Override
    public String id() {
        return MOCK_STRING;
    }

    @Override
    public String name() {
        return MOCK_STRING;
    }

    @Override
    public String resourceGroupName() {
        return MOCK_STRING;
    }

    @Override
    public RedisManager manager() {
        return null;
    }

    @Override
    public RedisCache refresh() {
        return null;
    }

    @Override
    public Mono<RedisCache> refreshAsync() {
        return null;
    }

    @Override
    public Update update() {
        return null;
    }

    @Override
    public RedisCachePremium asPremium() {
        return null;
    }

    @Override
    public boolean isPremium() {
        return false;
    }

    @Override
    public String provisioningState() {
        return MOCK_STRING;
    }

    @Override
    public String hostname() {
        return null;
    }

    @Override
    public int port() {
        return 0;
    }

    @Override
    public String redisVersion() {
        return MOCK_STRING;
    }

    @Override
    public Sku sku() {
        return null;
    }

    @Override
    public Map<String, String> redisConfiguration() {
        return null;
    }

    @Override
    public boolean nonSslPort() {
        return false;
    }

    @Override
    public int shardCount() {
        return 0;
    }

    @Override
    public String subnetId() {
        return MOCK_STRING;
    }

    @Override
    public String staticIp() {
        return MOCK_STRING;
    }

    @Override
    public RedisAccessKeys refreshKeys() {
        return new RedisAccessKeysMock();
    }

    @Override
    public RedisAccessKeys regenerateKey(RedisKeyType keyType) {
        return new RedisAccessKeysMock();
    }

    @Override
    public PublicNetworkAccess publicNetworkAccess() {
        return null;
    }

    @Override
    public TlsVersion minimumTlsVersion() {
        return null;
    }

    @Override
    public Map<String, RedisFirewallRule> firewallRules() {
        return null;
    }

    @Override
    public void forceReboot(RebootType arg0) {
    }

    @Override
    public List<ScheduleEntry> patchSchedules() {
        return null;
    }

    @Override
    public PagedIterable<PrivateEndpointConnection> listPrivateEndpointConnections() {
        return null;
    }

    @Override
    public PagedFlux<PrivateEndpointConnection> listPrivateEndpointConnectionsAsync() {
        return null;
    }

    @Override
    public PagedIterable<PrivateLinkResource> listPrivateLinkResources() {
        return null;
    }

    @Override
    public PagedFlux<PrivateLinkResource> listPrivateLinkResourcesAsync() {
        return null;
    }

    @Override
    public void approvePrivateEndpointConnection(String s) {

    }

    @Override
    public Mono<Void> approvePrivateEndpointConnectionAsync(String s) {
        return null;
    }

    @Override
    public void rejectPrivateEndpointConnection(String s) {

    }

    @Override
    public Mono<Void> rejectPrivateEndpointConnectionAsync(String s) {
        return null;
    }

    @Override
    public RedisResourceInner innerModel() {
        return null;
    }
}
