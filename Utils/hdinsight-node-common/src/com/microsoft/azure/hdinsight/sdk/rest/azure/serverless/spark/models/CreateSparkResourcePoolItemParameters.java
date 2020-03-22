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
package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Lake Analytics Spark Resource Pool creation request.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateSparkResourcePoolItemParameters {
    /**
     * Label for spark worker / master. Possible values include: 'SparkMaster', 'SparkWorker'.
     */
    @JsonProperty(value = "name")
    private SparkNodeType name;

    /**
     * Number of instances of spark master or spark worker.
     */
    @JsonProperty(value = "targetInstanceCount")
    private Integer targetInstanceCount;

    /**
     * Number of cores in each started instance of spark master or spark workers.
     */
    @JsonProperty(value = "perInstanceCoreCount")
    private Integer perInstanceCoreCount;

    /**
     * Allocated memory in GB for each started instance of spark master or spark workers.
     */
    @JsonProperty(value = "perInstanceMemoryInGB")
    private Integer perInstanceMemoryInGB;

    /**
     * Get label for spark worker / master. Possible values include: 'SparkMaster', 'SparkWorker'.
     *
     * @return the name value
     */
    public SparkNodeType name() {
        return this.name;
    }

    /**
     * Set label for spark worker / master. Possible values include: 'SparkMaster', 'SparkWorker'.
     *
     * @param name the name value to set
     * @return the CreateSparkResourcePoolItemParameters object itself.
     */
    public CreateSparkResourcePoolItemParameters withName(SparkNodeType name) {
        this.name = name;
        return this;
    }

    /**
     * Get number of instances of spark master or spark worker.
     *
     * @return the targetInstanceCount value
     */
    public Integer targetInstanceCount() {
        return this.targetInstanceCount;
    }

    /**
     * Set number of instances of spark master or spark worker.
     *
     * @param targetInstanceCount the targetInstanceCount value to set
     * @return the CreateSparkResourcePoolItemParameters object itself.
     */
    public CreateSparkResourcePoolItemParameters withTargetInstanceCount(Integer targetInstanceCount) {
        this.targetInstanceCount = targetInstanceCount;
        return this;
    }

    /**
     * Get number of cores in each started instance of spark master or spark workers.
     *
     * @return the perInstanceCoreCount value
     */
    public Integer perInstanceCoreCount() {
        return this.perInstanceCoreCount;
    }

    /**
     * Set number of cores in each started instance of spark master or spark workers.
     *
     * @param perInstanceCoreCount the perInstanceCoreCount value to set
     * @return the CreateSparkResourcePoolItemParameters object itself.
     */
    public CreateSparkResourcePoolItemParameters withPerInstanceCoreCount(Integer perInstanceCoreCount) {
        this.perInstanceCoreCount = perInstanceCoreCount;
        return this;
    }

    /**
     * Get allocated memory in GB for each started instance of spark master or spark workers.
     *
     * @return the perInstanceMemoryInGB value
     */
    public Integer perInstanceMemoryInGB() {
        return this.perInstanceMemoryInGB;
    }

    /**
     * Set allocated memory in GB for each started instance of spark master or spark workers.
     *
     * @param perInstanceMemoryInGB the perInstanceMemoryInGB value to set
     * @return the CreateSparkResourcePoolItemParameters object itself.
     */
    public CreateSparkResourcePoolItemParameters withPerInstanceMemoryInGB(Integer perInstanceMemoryInGB) {
        this.perInstanceMemoryInGB = perInstanceMemoryInGB;
        return this;
    }

}
