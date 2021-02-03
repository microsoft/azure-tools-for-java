/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Lake Analytics Spark Resource Pool update request parameters.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateSparkResourcePoolItemParameters {
    /**
     * Label for spark worker / master. Possible values include: 'SparkMaster', 'SparkWorker'.
     */
    @JsonProperty(value = "name")
    private SparkNodeType name;

    /**
     * Number of instances of spark worker.
     */
    @JsonProperty(value = "targetInstanceCount")
    private Integer targetInstanceCount;

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
     * @return the UpdateSparkResourcePoolItemParameters object itself.
     */
    public UpdateSparkResourcePoolItemParameters withName(SparkNodeType name) {
        this.name = name;
        return this;
    }

    /**
     * Get number of instances of spark worker.
     *
     * @return the targetInstanceCount value
     */
    public Integer targetInstanceCount() {
        return this.targetInstanceCount;
    }

    /**
     * Set number of instances of spark worker.
     *
     * @param targetInstanceCount the targetInstanceCount value to set
     * @return the UpdateSparkResourcePoolItemParameters object itself.
     */
    public UpdateSparkResourcePoolItemParameters withTargetInstanceCount(Integer targetInstanceCount) {
        this.targetInstanceCount = targetInstanceCount;
        return this;
    }

}
