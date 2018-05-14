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

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

/**
 * Data Lake Analytics Spark Resource Pool creation request.
 */
public class SparkResourcePoolItemProperties {
    /**
     * Label for spark worker or spark master. Possible values include: 'Spark
     * Master', 'Spark Worker'.
     */
    @NotNull
    @JsonProperty(value = "name", access = JsonProperty.Access.WRITE_ONLY)
    private String name;

    /**
     * Number of instances of spark master or spark worker.
     */
    @NotNull
    @JsonProperty(value = "targetInstanceCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer targetInstanceCount;

    /**
     * Number of cores in each started instance of spark master or spark
     * workers.
     */
    @NotNull
    @JsonProperty(value = "perInstanceCoreCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer perInstanceCoreCount;

    /**
     * Allocated memory in GB for each started instance of spark master or
     * spark workers.
     */
    @NotNull
    @JsonProperty(value = "perInstanceMemoryInGB", access = JsonProperty.Access.WRITE_ONLY)
    private Integer perInstanceMemoryInGB;

    /**
     * Guid represting the spark master or worker.
     */
    @NotNull
    @JsonProperty(value = "id", access = JsonProperty.Access.WRITE_ONLY)
    private UUID id;

    /**
     * Status of the spark resource pool.
     */
    @NotNull
    @JsonProperty(value = "status", access = JsonProperty.Access.WRITE_ONLY)
    private String status;

    /**
     * Url which can be used by clients to track the resource pool.
     */
    @NotNull
    @JsonProperty(value = "trackingUrl", access = JsonProperty.Access.WRITE_ONLY)
    private String trackingUrl;

    /**
     * Number of instances running.
     */
    @NotNull
    @JsonProperty(value = "runningInstanceCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer runningInstanceCount;

    /**
     * Number of instances yet to be launched.
     */
    @NotNull
    @JsonProperty(value = "outstandingInstanceCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer outstandingInstanceCount;

    /**
     * Number of instances that failed to launch.
     */
    @NotNull
    @JsonProperty(value = "failedInstanceCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer failedInstanceCount;

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
     * Get the targetInstanceCount value.
     *
     * @return the targetInstanceCount value
     */
    @NotNull
    public Integer getTargetInstanceCount() {
        return this.targetInstanceCount;
    }

    /**
     * Get the perInstanceCoreCount value.
     *
     * @return the perInstanceCoreCount value
     */
    @NotNull
    public Integer getPerInstanceCoreCount() {
        return this.perInstanceCoreCount;
    }

    /**
     * Get the perInstanceMemoryInGB value.
     *
     * @return the perInstanceMemoryInGB value
     */
    @NotNull
    public Integer getPerInstanceMemoryInGB() {
        return this.perInstanceMemoryInGB;
    }

    /**
     * Get the id value.
     *
     * @return the id value
     */
    @NotNull
    public UUID getId() {
        return this.id;
    }

    /**
     * Get the status value.
     *
     * @return the status value
     */
    @NotNull
    public String getStatus() {
        return this.status;
    }

    /**
     * Get the trackingUrl value.
     *
     * @return the trackingUrl value
     */
    @NotNull
    public String getTrackingUrl() {
        return this.trackingUrl;
    }

    /**
     * Get the runningInstanceCount value.
     *
     * @return the runningInstanceCount value
     */
    @NotNull
    public Integer getRunningInstanceCount() {
        return this.runningInstanceCount;
    }

    /**
     * Get the outstandingInstanceCount value.
     *
     * @return the outstandingInstanceCount value
     */
    @NotNull
    public Integer getOutstandingInstanceCount() {
        return this.outstandingInstanceCount;
    }

    /**
     * Get the failedInstanceCount value.
     *
     * @return the failedInstanceCount value
     */
    @NotNull
    public Integer getFailedInstanceCount() {
        return this.failedInstanceCount;
    }

}
