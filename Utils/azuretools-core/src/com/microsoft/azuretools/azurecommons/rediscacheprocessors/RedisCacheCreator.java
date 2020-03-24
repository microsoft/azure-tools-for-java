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

import java.util.HashMap;
import java.util.Map;

import com.microsoft.azure.management.redis.RedisCaches;

public final class RedisCacheCreator {
    private Map<String, ProcessingStrategy> creatorMap = new HashMap<String, ProcessingStrategy>();

    private static final String BASIC = "BASIC";
    private static final String STANDARD = "STD";
    private static final String PREMIUM = "PREMIUM";

    private static final String NEW_NO_SSL = "NewNoSSL";
    private static final String NEW = "New";
    private static final String EXISTING_NO_SSL = "ExistingNoSSL";
    private static final String EXISTING = "Existing";


    private void initCreatorsForBasicTier(RedisCaches redisCaches, String dnsName, String regionName, String groupName, int[] capacities) {
        for (int capacity : capacities) {
            //e.g. "BASIC0NewNoSSL"
            creatorMap.put(BASIC + Integer.toString(capacity) + NEW_NO_SSL, new BasicWithNewResGrpNonSsl(redisCaches, dnsName, regionName, groupName, capacity));
            creatorMap.put(BASIC + Integer.toString(capacity) + NEW, new BasicWithNewResGrp(redisCaches, dnsName, regionName, groupName, capacity));
            creatorMap.put(BASIC + Integer.toString(capacity) + EXISTING_NO_SSL, new BasicWithExistResGrpNonSsl(redisCaches, dnsName, regionName, groupName, capacity));
            creatorMap.put(BASIC + Integer.toString(capacity) + EXISTING, new BasicWithExistResGrp(redisCaches, dnsName, regionName, groupName, capacity));
        }
    }

    private void initCreatorsForStdTier(RedisCaches redisCaches, String dnsName, String regionName, String groupName, int[] capacities) {
        for (int capacity : capacities) {
            //e.g. "STD0NewNoSSL"
            creatorMap.put(STANDARD + Integer.toString(capacity) + NEW_NO_SSL, new StdWithNewResGrpNonSsl(redisCaches, dnsName, regionName, groupName, capacity));
            creatorMap.put(STANDARD + Integer.toString(capacity) + NEW, new StdWithNewResGrp(redisCaches, dnsName, regionName, groupName, capacity));
            creatorMap.put(STANDARD + Integer.toString(capacity) + EXISTING_NO_SSL, new StdWithExistResGrpNonSsl(redisCaches, dnsName, regionName, groupName, capacity));
            creatorMap.put(STANDARD + Integer.toString(capacity) + EXISTING, new StdWithExistResGrp(redisCaches, dnsName, regionName, groupName, capacity));
        }
    }

    private void initCreatorsForPremiumTier(RedisCaches redisCaches, String dnsName, String regionName, String groupName, int[] capacities) {
        for (int capacity : capacities) {
            //e.g. "PREMIUM0NewNoSSL"
            creatorMap.put(PREMIUM + Integer.toString(capacity) + NEW_NO_SSL, new PremWithNewResGrpNonSsl(redisCaches, dnsName, regionName, groupName, capacity));
            creatorMap.put(PREMIUM + Integer.toString(capacity) + NEW, new PremWithNewResGrp(redisCaches, dnsName, regionName, groupName, capacity));
            creatorMap.put(PREMIUM + Integer.toString(capacity) + EXISTING_NO_SSL, new PremWithExistResGrpNonSsl(redisCaches, dnsName, regionName, groupName, capacity));
            creatorMap.put(PREMIUM + Integer.toString(capacity) + EXISTING, new PremWithExistResGrp(redisCaches, dnsName, regionName, groupName, capacity));
        }
    }
    public RedisCacheCreator(RedisCaches redisCaches, String dnsName, String regionName, String groupName) {
        initCreatorsForBasicTier(redisCaches, dnsName, regionName, groupName, new int[] {0, 1, 2, 3, 4, 5, 6});
        initCreatorsForStdTier(redisCaches, dnsName, regionName, groupName, new int[] {0, 1, 2, 3, 4, 5, 6});
        initCreatorsForPremiumTier(redisCaches, dnsName, regionName, groupName, new int[] {1, 2, 3, 4});
    }

    public Map<String, ProcessingStrategy> CreatorMap() {
        if(creatorMap.isEmpty()) {
            throw new IllegalStateException("Redis cache creator map not initialized properly");
        }
        return creatorMap;
    }
}
