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

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Spark specific resource pool information.
 */
public class SparkResourcePoolProperties {
    /**
     * Version of the template used while deploying the resource pool.
     */
    @JsonProperty(value = "resourcePoolVersion", access = JsonProperty.Access.WRITE_ONLY)
    private String resourcePoolVersion;

    /**
     * Spark version to be deployed on the instances of the resource pool.
     */
    @JsonProperty(value = "sparkVersion", access = JsonProperty.Access.WRITE_ONLY)
    private String sparkVersion;

    /**
     * ADLS directory path to store Spark events and logs.
     */
    @JsonProperty(value = "sparkEventsDirectoryPath")
    private String sparkEventsDirectoryPath;

    /**
     * The sparkResourceCollection property.
     */
    @JsonProperty(value = "sparkResourceCollection", access = JsonProperty.Access.WRITE_ONLY)
    private List<SparkResourcePoolItemProperties> sparkResourceCollection;

    /**
     * Stte of the Activity. Possible values include: 'New', 'Queued',
     * 'Scheduling', 'Starting', 'Launching', 'Running', 'Rediscovering',
     * 'Ending', 'Ended'.
     */
    @JsonProperty(value = "state")
    private ResourcePoolState state;

    /**
     * Definition of Spark Uri Collection.
     */
    @JsonProperty(value = "sparkUriCollection", access = JsonProperty.Access.WRITE_ONLY)
    private SparkResourcePoolUriItemProperties sparkUriCollection;

    /**
     * Get the resourcePoolVersion value.
     *
     * @return the resourcePoolVersion value
     */
    public String resourcePoolVersion() {
        return this.resourcePoolVersion;
    }

    /**
     * Get the sparkVersion value.
     *
     * @return the sparkVersion value
     */
    public String sparkVersion() {
        return this.sparkVersion;
    }

    /**
     * Get the sparkEventsDirectoryPath value.
     *
     * @return the sparkEventsDirectoryPath value
     */
    public String sparkEventsDirectoryPath() {
        return this.sparkEventsDirectoryPath;
    }

    /**
     * Set the sparkEventsDirectoryPath value.
     *
     * @param sparkEventsDirectoryPath the sparkEventsDirectoryPath value to set
     * @return the SparkResourcePoolProperties object itself.
     */
    public SparkResourcePoolProperties withSparkEventsDirectoryPath(String sparkEventsDirectoryPath) {
        this.sparkEventsDirectoryPath = sparkEventsDirectoryPath;
        return this;
    }

    /**
     * Get the sparkResourceCollection value.
     *
     * @return the sparkResourceCollection value
     */
    public List<SparkResourcePoolItemProperties> sparkResourceCollection() {
        return this.sparkResourceCollection;
    }

    /**
     * Get the state value.
     *
     * @return the state value
     */
    public ResourcePoolState state() {
        return this.state;
    }

    /**
     * Set the state value.
     *
     * @param state the state value to set
     * @return the SparkResourcePoolProperties object itself.
     */
    public SparkResourcePoolProperties withState(ResourcePoolState state) {
        this.state = state;
        return this;
    }

    /**
     * Get the sparkUriCollection value.
     *
     * @return the sparkUriCollection value
     */
    public SparkResourcePoolUriItemProperties sparkUriCollection() {
        return this.sparkUriCollection;
    }

}
