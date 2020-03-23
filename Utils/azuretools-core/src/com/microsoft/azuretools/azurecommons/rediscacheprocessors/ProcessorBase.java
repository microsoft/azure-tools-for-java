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
