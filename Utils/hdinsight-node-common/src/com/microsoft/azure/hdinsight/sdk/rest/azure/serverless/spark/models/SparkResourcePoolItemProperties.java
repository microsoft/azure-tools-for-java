/**
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

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Lake Analytics Spark Resource Pool creation request.
 */
public class SparkResourcePoolItemProperties {
    /**
     * Label for spark worker or spark master. Possible values include:
     * 'SparkMaster', 'SparkWorker'.
     */
    @JsonProperty(value = "name")
    private SparkNodeType name;

    /**
     * Number of instances of spark master or spark worker.
     */
    @JsonProperty(value = "targetInstanceCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer targetInstanceCount;

    /**
     * Number of cores in each started instance of spark master or spark
     * workers.
     */
    @JsonProperty(value = "perInstanceCoreCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer perInstanceCoreCount;

    /**
     * Allocated memory in GB for each started instance of spark master or
     * spark workers.
     */
    @JsonProperty(value = "perInstanceMemoryInGB", access = JsonProperty.Access.WRITE_ONLY)
    private Integer perInstanceMemoryInGB;

    /**
     * Guid represting the spark master or worker.
     */
    @JsonProperty(value = "id", access = JsonProperty.Access.WRITE_ONLY)
    private UUID id;

    /**
     * Status of the spark resource pool.
     */
    @JsonProperty(value = "status", access = JsonProperty.Access.WRITE_ONLY)
    private String status;

    /**
     * Number of instances running.
     */
    @JsonProperty(value = "runningInstanceCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer runningInstanceCount;

    /**
     * Number of instances yet to be launched.
     */
    @JsonProperty(value = "outstandingInstanceCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer outstandingInstanceCount;

    /**
     * Number of instances that failed to launch.
     */
    @JsonProperty(value = "failedInstanceCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer failedInstanceCount;

    /**
     * Get the name value.
     *
     * @return the name value
     */
    public SparkNodeType name() {
        return this.name;
    }

    /**
     * Set the name value.
     *
     * @param name the name value to set
     * @return the SparkResourcePoolItemProperties object itself.
     */
    public SparkResourcePoolItemProperties withName(SparkNodeType name) {
        this.name = name;
        return this;
    }

    /**
     * Get the targetInstanceCount value.
     *
     * @return the targetInstanceCount value
     */
    public Integer targetInstanceCount() {
        return this.targetInstanceCount;
    }

    /**
     * Get the perInstanceCoreCount value.
     *
     * @return the perInstanceCoreCount value
     */
    public Integer perInstanceCoreCount() {
        return this.perInstanceCoreCount;
    }

    /**
     * Get the perInstanceMemoryInGB value.
     *
     * @return the perInstanceMemoryInGB value
     */
    public Integer perInstanceMemoryInGB() {
        return this.perInstanceMemoryInGB;
    }

    /**
     * Get the id value.
     *
     * @return the id value
     */
    public UUID id() {
        return this.id;
    }

    /**
     * Get the status value.
     *
     * @return the status value
     */
    public String status() {
        return this.status;
    }

    /**
     * Get the runningInstanceCount value.
     *
     * @return the runningInstanceCount value
     */
    public Integer runningInstanceCount() {
        return this.runningInstanceCount;
    }

    /**
     * Get the outstandingInstanceCount value.
     *
     * @return the outstandingInstanceCount value
     */
    public Integer outstandingInstanceCount() {
        return this.outstandingInstanceCount;
    }

    /**
     * Get the failedInstanceCount value.
     *
     * @return the failedInstanceCount value
     */
    public Integer failedInstanceCount() {
        return this.failedInstanceCount;
    }

}
