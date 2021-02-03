/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azurecommons.rediscacheprocessors;

import com.microsoft.azure.management.redis.RedisCaches;

import com.microsoft.azure.management.redis.RedisCache.DefinitionStages.WithGroup;

public abstract class ProcessorBase{
    private RedisCaches redisCachesInst;
    private String dnsName;
    private String regionName;
    private String resGrpName;
    private int capacity;
    private boolean nonSslPort;

    public RedisCaches RedisCachesInstance() {
        return redisCachesInst;
    }

    public String DNSName() {
        return dnsName;
    }

    public String RegionName() {
        return regionName;
    }

    public String ResourceGroupName() {
        return resGrpName;
    }

    public int Capacity() {
        return capacity;
    }

    public boolean NonSslPort() {
        return nonSslPort;
    }

    protected ProcessorBase withRedisCaches(RedisCaches redisCachesInst) {
        this.redisCachesInst = redisCachesInst;
        return this;
    }

    protected ProcessorBase withDNSName(String dnsName) {
        this.dnsName = dnsName;
        return this;
    }

    protected ProcessorBase withRegion(String regionName) {
        this.regionName = regionName;
        return this;
    }

    protected ProcessorBase withGroup(String resGrpName) {
        this.resGrpName = resGrpName;
        return this;
    }

    protected ProcessorBase withCapacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    protected ProcessorBase withNonSslPort(boolean nonSslPort)
    {
        this.nonSslPort = nonSslPort;
        return this;
    }

    protected WithGroup withDNSNameAndRegionDefinition() {
        return this.RedisCachesInstance()
                .define(this.DNSName())
                .withRegion(this.RegionName());
    }
}
