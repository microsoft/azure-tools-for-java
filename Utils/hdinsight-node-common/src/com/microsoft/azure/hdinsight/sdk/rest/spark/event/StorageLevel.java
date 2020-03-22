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
