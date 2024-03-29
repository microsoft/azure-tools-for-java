/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azurecommons.rediscacheprocessors;

import com.azure.resourcemanager.redis.models.RedisCache;
import com.azure.resourcemanager.redis.models.RedisCaches;
import com.azure.resourcemanager.resources.fluentcore.model.Creatable;

public class PremWithNewResGrpNonSsl extends ProcessorBaseImpl {

    public PremWithNewResGrpNonSsl(RedisCaches rediscaches, String dns, String regionName,
            String group, int capacity) {
        super(rediscaches, dns, regionName, group, capacity);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ProcessingStrategy process() {
        Creatable<RedisCache> redisCacheDefinition = withDNSNameAndRegionDefinition()
                .withNewResourceGroup(this.ResourceGroupName())
                .withPremiumSku(this.Capacity())
                .withNonSslPort();
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
