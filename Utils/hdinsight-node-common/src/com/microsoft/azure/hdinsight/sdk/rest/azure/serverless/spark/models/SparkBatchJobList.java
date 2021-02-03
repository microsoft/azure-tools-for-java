/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * List of spark batch jobs.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SparkBatchJobList {
    /**
     * the list of spark batch jobs.
     */
    @JsonProperty(value = "value", access = JsonProperty.Access.WRITE_ONLY)
    private List<SparkBatchJob> value;

    /**
     * The nextLink property.
     */
    @JsonProperty(value = "nextLink")
    private String nextLink;

    /**
     * The count property.
     */
    @JsonProperty(value = "count")
    private Integer count;

    /**
     * Get the list of spark batch jobs.
     *
     * @return the value value
     */
    public List<SparkBatchJob> value() {
        return this.value;
    }

    /**
     * Get the nextLink value.
     *
     * @return the nextLink value
     */
    public String nextLink() {
        return this.nextLink;
    }

    /**
     * Set the nextLink value.
     *
     * @param nextLink the nextLink value to set
     * @return the SparkBatchJobList object itself.
     */
    public SparkBatchJobList withNextLink(String nextLink) {
        this.nextLink = nextLink;
        return this;
    }

    /**
     * Get the count value.
     *
     * @return the count value
     */
    public Integer count() {
        return this.count;
    }

    /**
     * Set the count value.
     *
     * @param count the count value to set
     * @return the SparkBatchJobList object itself.
     */
    public SparkBatchJobList withCount(Integer count) {
        this.count = count;
        return this;
    }

}
