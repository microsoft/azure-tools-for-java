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


import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

/**
 * Spark specific resource pool information.
 */
public class CreateSparkResourcePoolParameters {
    /**
     * Name for the spark resource pool.
     */
    @NotNull
    @JsonProperty(value = "name")
    private String name;

    /**
     * Version of the template used while deploying the resource pool.
     */
    @NotNull
    @JsonProperty(value = "resourcePoolVersion")
    private String resourcePoolVersion;

    /**
     * Spark version to be deployed on the instances of the resource pool.
     */
    @NotNull
    @JsonProperty(value = "sparkVersion")
    private String sparkVersion;

    /**
     * ADLS directory path to store Spark Events.
     */
    @NotNull
    @JsonProperty(value = "sparkEventsDirectoryPath")
    private String sparkEventsDirectoryPath;

    /**
     * Definition of spark master and spark workers.
     */
    @NotNull
    @JsonProperty(value = "sparkResourceCollection")
    private List<CreateSparkResourcePoolItemParameters> sparkResourceCollection;

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
     * Get the resourcePoolVersion value.
     *
     * @return the resourcePoolVersion value
     */
    @NotNull
    public String getResourcePoolVersion() {
        return this.resourcePoolVersion;
    }

    /**
     * Set the resourcePoolVersion value.
     *
     * @param resourcePoolVersion the resourcePoolVersion value to set
     */
    public void setResourcePoolVersion(@NotNull String resourcePoolVersion) {
        this.resourcePoolVersion = resourcePoolVersion;
    }

    /**
     * Get the sparkVersion value.
     *
     * @return the sparkVersion value
     */
    @NotNull
    public String getSparkVersion() {
        return this.sparkVersion;
    }

    /**
     * Set the sparkVersion value.
     *
     * @param sparkVersion the sparkVersion value to set
     */
    public void setSparkVersion(@NotNull String sparkVersion) {
        this.sparkVersion = sparkVersion;
    }

    /**
     * Get the sparkEventsDirectoryPath value.
     *
     * @return the sparkEventsDirectoryPath value
     */
    @NotNull
    public String getSparkEventsDirectoryPath() {
        return this.sparkEventsDirectoryPath;
    }

    /**
     * Set the sparkEventsDirectoryPath value.
     *
     * @param sparkEventsDirectoryPath the sparkEventsDirectoryPath value to set
     */
    public void setSparkEventsDirectoryPath(@NotNull String sparkEventsDirectoryPath) {
        this.sparkEventsDirectoryPath = sparkEventsDirectoryPath;
    }

    /**
     * Get the sparkResourceCollection value.
     *
     * @return the sparkResourceCollection value
     */
    @NotNull
    public List<CreateSparkResourcePoolItemParameters> getSparkResourceCollection() {
        return this.sparkResourceCollection;
    }

    /**
     * Set the sparkResourceCollection value.
     *
     * @param sparkResourceCollection the sparkResourceCollection value to set
     */
    public void setSparkResourceCollection(@NotNull List<CreateSparkResourcePoolItemParameters> sparkResourceCollection) {
        this.sparkResourceCollection = sparkResourceCollection;
    }

}
