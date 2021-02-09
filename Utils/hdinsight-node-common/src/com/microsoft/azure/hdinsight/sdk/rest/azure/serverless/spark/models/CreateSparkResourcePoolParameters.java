/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Spark specific resource pool information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateSparkResourcePoolParameters {
    /**
     * Version of the template used while deploying the resource pool.
     */
    @JsonProperty(value = "resourcePoolVersion")
    private String resourcePoolVersion;

    /**
     * Spark version to be deployed on the instances of the resource pool.
     */
    @JsonProperty(value = "sparkVersion")
    private String sparkVersion;

    /**
     * ADLS directory path to store Spark events and logs.
     */
    @JsonProperty(value = "sparkEventsDirectoryPath", required = true)
    private String sparkEventsDirectoryPath;

    /**
     * Definition of spark master and spark workers.
     */
    @JsonProperty(value = "sparkResourceCollection")
    private List<CreateSparkResourcePoolItemParameters> sparkResourceCollection;

    /**
     * Special properties that will allow choosing/targeting of features (runtime, gp version etc) on server side.
     */
    @JsonProperty(value = "extendedProperties")
    private Map<String, String> extendedProperties;

    /**
     * Get version of the template used while deploying the resource pool.
     *
     * @return the resourcePoolVersion value
     */
    public String resourcePoolVersion() {
        return this.resourcePoolVersion;
    }

    /**
     * Set version of the template used while deploying the resource pool.
     *
     * @param resourcePoolVersion the resourcePoolVersion value to set
     * @return the CreateSparkResourcePoolParameters object itself.
     */
    public CreateSparkResourcePoolParameters withResourcePoolVersion(String resourcePoolVersion) {
        this.resourcePoolVersion = resourcePoolVersion;
        return this;
    }

    /**
     * Get spark version to be deployed on the instances of the resource pool.
     *
     * @return the sparkVersion value
     */
    public String sparkVersion() {
        return this.sparkVersion;
    }

    /**
     * Set spark version to be deployed on the instances of the resource pool.
     *
     * @param sparkVersion the sparkVersion value to set
     * @return the CreateSparkResourcePoolParameters object itself.
     */
    public CreateSparkResourcePoolParameters withSparkVersion(String sparkVersion) {
        this.sparkVersion = sparkVersion;
        return this;
    }

    /**
     * Get aDLS directory path to store Spark events and logs.
     *
     * @return the sparkEventsDirectoryPath value
     */
    public String sparkEventsDirectoryPath() {
        return this.sparkEventsDirectoryPath;
    }

    /**
     * Set aDLS directory path to store Spark events and logs.
     *
     * @param sparkEventsDirectoryPath the sparkEventsDirectoryPath value to set
     * @return the CreateSparkResourcePoolParameters object itself.
     */
    public CreateSparkResourcePoolParameters withSparkEventsDirectoryPath(String sparkEventsDirectoryPath) {
        this.sparkEventsDirectoryPath = sparkEventsDirectoryPath;
        return this;
    }

    /**
     * Get definition of spark master and spark workers.
     *
     * @return the sparkResourceCollection value
     */
    public List<CreateSparkResourcePoolItemParameters> sparkResourceCollection() {
        return this.sparkResourceCollection;
    }

    /**
     * Set definition of spark master and spark workers.
     *
     * @param sparkResourceCollection the sparkResourceCollection value to set
     * @return the CreateSparkResourcePoolParameters object itself.
     */
    public CreateSparkResourcePoolParameters withSparkResourceCollection(List<CreateSparkResourcePoolItemParameters> sparkResourceCollection) {
        this.sparkResourceCollection = sparkResourceCollection;
        return this;
    }

    /**
     * Get special properties that will allow choosing/targeting of features (runtime, gp version etc) on server side.
     *
     * @return the extendedProperties value
     */
    public Map<String, String> extendedProperties() {
        return this.extendedProperties;
    }

    /**
     * Set special properties that will allow choosing/targeting of features (runtime, gp version etc) on server side.
     *
     * @param extendedProperties the extendedProperties value to set
     * @return the CreateSparkResourcePoolParameters object itself.
     */
    public CreateSparkResourcePoolParameters withExtendedProperties(Map<String, String> extendedProperties) {
        this.extendedProperties = extendedProperties;
        return this;
    }

}
