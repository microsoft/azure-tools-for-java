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

package com.microsoft.azure.hdinsight.sdk.rest.spark.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RDDInfo {

    @JsonProperty("RDD ID")
    private int rddId;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Scope")
    private String Scope;

    @JsonProperty("Callsite")
    private String callSite;

    @JsonProperty("Parent IDs")
    private int [] parentIds;

    @JsonProperty("Number of Partitions")
    private int numberOfPartitions;

    @JsonProperty("Number of Cached Partitions")
    private int numberOfCachedPartitions;

    @JsonProperty("Memory Size")
    private long memorySize;

    @JsonProperty("Disk Size")
    private long diskSize;

    @JsonProperty("Storage Level")
    private StorageLevel storageLevels;

    public int getRddId() {
        return rddId;
    }

    public void setRddId(int rddId) {
        this.rddId = rddId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScope() {
        return Scope;
    }

    public void setScope(String scope) {
        Scope = scope;
    }

    public String getCallSite() {
        return callSite;
    }

    public void setCallSite(String callSite) {
        this.callSite = callSite;
    }

    public int[] getParentIds() {
        return parentIds;
    }

    public void setParentIds(int[] parentIds) {
        this.parentIds = parentIds;
    }

    public int getNumberOfPartitions() {
        return numberOfPartitions;
    }

    public void setNumberOfPartitions(int numberOfPartitions) {
        this.numberOfPartitions = numberOfPartitions;
    }

    public int getNumberOfCachedPartitions() {
        return numberOfCachedPartitions;
    }

    public void setNumberOfCachedPartitions(int numberOfCachedPartitions) {
        this.numberOfCachedPartitions = numberOfCachedPartitions;
    }

    public long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(long memorySize) {
        this.memorySize = memorySize;
    }

    public long getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(long diskSize) {
        this.diskSize = diskSize;
    }

    public StorageLevel getStorageLevels() {
        return storageLevels;
    }

    public void setStorageLevels(StorageLevel storageLevels) {
        this.storageLevels = storageLevels;
    }
}
