/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azurecommons.rediscacheprocessors;

import java.util.HashMap;
import java.util.Map;

import com.azure.resourcemanager.redis.models.RedisCaches;

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
