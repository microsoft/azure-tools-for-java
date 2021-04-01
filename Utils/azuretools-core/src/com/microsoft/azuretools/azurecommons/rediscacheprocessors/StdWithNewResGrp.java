/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azurecommons.rediscacheprocessors;

import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azure.management.redis.RedisCaches;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;

public class StdWithNewResGrp extends ProcessorBaseImpl {

    public StdWithNewResGrp(RedisCaches rediscaches, String dns, String regionName, String group,
            int capacity) {
        super(rediscaches, dns, regionName, group, capacity);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ProcessingStrategy process() {
        Creatable<RedisCache> redisCacheDefinition = withDNSNameAndRegionDefinition()
                .withNewResourceGroup(this.ResourceGroupName())
                .withStandardSku(this.Capacity());
        this.RedisCachesInstance().create(redisCacheDefinition);
        return this;
    }

    @Override
    public void waitForCompletion(String produce) throws InterruptedException {
        queue.put(produce);
    }
    @Override
    public void notifyCompletion() throws InterruptedException {
        queue.take();
    }
}
