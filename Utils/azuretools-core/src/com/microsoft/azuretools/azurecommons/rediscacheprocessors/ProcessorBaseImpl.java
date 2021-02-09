/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azurecommons.rediscacheprocessors;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import com.microsoft.azure.management.redis.RedisCaches;

public abstract class ProcessorBaseImpl extends ProcessorBase implements ProcessingStrategy {

    protected BlockingQueue<String> queue;

    public ProcessorBaseImpl(RedisCaches rediscaches, String dns, String regionName, String group, int capacity) throws IllegalArgumentException {
        if (rediscaches == null || dns == null || regionName == null || group == null) {
            throw new IllegalArgumentException("All parameters are required and cannot be null.");
        }
        queue = new SynchronousQueue<String>();
        this.withRedisCaches(rediscaches).withGroup(group).withRegion(regionName).withDNSName(dns).withCapacity(capacity);
    }
    @Override
    public abstract ProcessingStrategy process() throws InterruptedException;

    @Override
    public abstract void waitForCompletion(String produce) throws InterruptedException;

    @Override
    public abstract void notifyCompletion() throws InterruptedException;
}
