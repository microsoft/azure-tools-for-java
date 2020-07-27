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
