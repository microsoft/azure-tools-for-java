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
package com.microsoft.azuretools.azurecommons.rediscacheprocessors;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.azure.management.redis.RedisCaches;

public final class RedisCacheCreator {
    private Map<String, ProcessingStrategy> creatorMap = new HashMap<String, ProcessingStrategy>();
    
    private void initCreatorsForBasicTier(RedisCaches redisCaches, String dnsName, String regionName, String groupName, int[] capacities) {
    	for (int capacity : capacities) {
    		//e.g. "BASIC0NewNoSSL"
    		creatorMap.put("BASIC" + Integer.toString(capacity) + "NewNoSSL", new BasicWithNewResGrpNonSsl(redisCaches, dnsName, regionName, groupName, capacity));
    		creatorMap.put("BASIC" + Integer.toString(capacity) + "New", new BasicWithNewResGrp(redisCaches, dnsName, regionName, groupName, capacity));
    		creatorMap.put("BASIC" + Integer.toString(capacity) + "ExistingNoSSL", new BasicWithExistResGrpNonSsl(redisCaches, dnsName, regionName, groupName, capacity));
    		creatorMap.put("BASIC" + Integer.toString(capacity) + "Existing", new BasicWithExistResGrp(redisCaches, dnsName, regionName, groupName, capacity));
    	}
    }
    
    private void initCreatorsForStdTier(RedisCaches redisCaches, String dnsName, String regionName, String groupName, int[] capacities) {
    	for (int capacity : capacities) {
    		//e.g. "STD0NewNoSSL"
    		creatorMap.put("STD" + Integer.toString(capacity) + "NewNoSSL", new StdWithNewResGrpNonSsl(redisCaches, dnsName, regionName, groupName, capacity));
    		creatorMap.put("STD" + Integer.toString(capacity) + "New", new StdWithNewResGrp(redisCaches, dnsName, regionName, groupName, capacity));
    		creatorMap.put("STD" + Integer.toString(capacity) + "ExistingNoSSL", new StdWithExistResGrpNonSsl(redisCaches, dnsName, regionName, groupName, capacity));
    		creatorMap.put("STD" + Integer.toString(capacity) + "Existing", new StdWithExistResGrp(redisCaches, dnsName, regionName, groupName, capacity));
    	}
    }
    
    private void initCreatorsForPremiumTier(RedisCaches redisCaches, String dnsName, String regionName, String groupName, int[] capacities) {
    	for (int capacity : capacities) {
    		//e.g. "PREMIUM0NewNoSSL"
    		creatorMap.put("PREMIUM" + Integer.toString(capacity) + "NewNoSSL", new PremWithNewResGrpNonSsl(redisCaches, dnsName, regionName, groupName, capacity));
    		creatorMap.put("PREMIUM" + Integer.toString(capacity) + "New", new PremWithNewResGrp(redisCaches, dnsName, regionName, groupName, capacity));
    		creatorMap.put("PREMIUM" + Integer.toString(capacity) + "ExistingNoSSL", new PreWithExistResGrpNonSsl(redisCaches, dnsName, regionName, groupName, capacity));
    		creatorMap.put("PREMIUM" + Integer.toString(capacity) + "Existing", new PremWithExistResGrp(redisCaches, dnsName, regionName, groupName, capacity));
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