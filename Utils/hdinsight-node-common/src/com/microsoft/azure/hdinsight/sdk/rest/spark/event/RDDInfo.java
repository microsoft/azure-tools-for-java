/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.spark.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RDDInfo {

    @JsonProperty("RDD ID")
    private int rddId;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Scope")
    private String scope;

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
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
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
