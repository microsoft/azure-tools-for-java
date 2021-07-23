/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.spark.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StorageLevel {
    @JsonProperty("Use Disk")
    private boolean isUseDisk;

    @JsonProperty("Use Memory")
    private boolean isUseMemory;

    @JsonProperty("Deserialized")
    private boolean isDeserialized;

    @JsonProperty("Replication")
    private int replication;

    public boolean isUseDisk() {
        return isUseDisk;
    }

    public void setUseDisk(boolean useDisk) {
        isUseDisk = useDisk;
    }

    public boolean isUseMemory() {
        return isUseMemory;
    }

    public void setUseMemory(boolean useMemory) {
        isUseMemory = useMemory;
    }

    public boolean isDeserialized() {
        return isDeserialized;
    }

    public void setDeserialized(boolean deserialized) {
        isDeserialized = deserialized;
    }

    public int getReplication() {
        return replication;
    }

    public void setReplication(int replication) {
        this.replication = replication;
    }
}
