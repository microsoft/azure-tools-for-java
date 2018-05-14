/*
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

package com.microsoft.azure.hdinsight.sdk.rest.sparkserverless;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

/**
 * Data Lake Analytics Spark Resource Pool creation request.
 */
public class CreateSparkResourcePoolItemParameters {
    /**
     * Name for the spark master or spark workers.
     */
    @NotNull
    @JsonProperty(value = "name")
    private String name;

    /**
     * Number of instances of spark master or spark worker.
     */
    @NotNull
    @JsonProperty(value = "targetInstanceCount")
    private int targetInstanceCount;

    /**
     * Number of cores in each started instance of spark master or spark
     * workers.
     */
    @NotNull
    @JsonProperty(value = "perInstanceCoreCount")
    private int perInstanceCoreCount;

    /**
     * Allocated memory in GB for each started instance of spark master or
     * spark workers.
     */
    @NotNull
    @JsonProperty(value = "perInstanceMemoryInGB")
    private int perInstanceMemoryInGB;

    /**
     * Get the name value.
     *
     * @return the name value
     */
    @NotNull
    public String getName() {
        return this.name;
    }

    /**
     * Set the name value.
     *
     * @param name the name value to set
     */
    public void setName(@NotNull String name) {
        this.name = name;
    }

    /**
     * Get the targetInstanceCount value.
     *
     * @return the targetInstanceCount value
     */
    @NotNull
    public int getTargetInstanceCount() {
        return this.targetInstanceCount;
    }

    /**
     * Set the targetInstanceCount value.
     *
     * @param targetInstanceCount the targetInstanceCount value to set
     */
    public void setTargetInstanceCount(@NotNull int targetInstanceCount) {
        this.targetInstanceCount = targetInstanceCount;
    }

    /**
     * Get the perInstanceCoreCount value.
     *
     * @return the perInstanceCoreCount value
     */
    @NotNull
    public int getPerInstanceCoreCount() {
        return this.perInstanceCoreCount;
    }

    /**
     * Set the perInstanceCoreCount value.
     *
     * @param perInstanceCoreCount the perInstanceCoreCount value to set
     */
    public void setPerInstanceCoreCount(@NotNull int perInstanceCoreCount) {
        this.perInstanceCoreCount = perInstanceCoreCount;
    }

    /**
     * Get the perInstanceMemoryInGB value.
     *
     * @return the perInstanceMemoryInGB value
     */
    @NotNull
    public int getPerInstanceMemoryInGB() {
        return this.perInstanceMemoryInGB;
    }

    /**
     * Set the perInstanceMemoryInGB value.
     *
     * @param perInstanceMemoryInGB the perInstanceMemoryInGB value to set
     */
    public void setPerInstanceMemoryInGB(@NotNull int perInstanceMemoryInGB) {
        this.perInstanceMemoryInGB = perInstanceMemoryInGB;
    }

}
