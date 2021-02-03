/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

/**
 * Parameters used to submit a new Data Lake Analytics spark batch job creation request.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateSparkBatchJob implements IConvertible {
    /**
     * Friendly name of the spark batch job to submit.
     */
    @JsonProperty(value = "name", required = true)
    private String name;

    /**
     * Priority of the spark batch job in the account queue. Default is 1000, same as Scope jobs.
     */
    @JsonProperty(value = "priority")
    private Integer priority;

    /**
     * Pipeline and recurrence parameters.
     */
    @JsonProperty(value = "related")
    private JobRelationshipParameters related;

    /**
     * The spark batch job specific properties.
     */
    @JsonProperty(value = "properties", required = true)
    private CreateSparkBatchJobParameters properties;

    /**
     * Get friendly name of the spark batch job to submit.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Set friendly name of the spark batch job to submit.
     *
     * @param name the name value to set
     * @return the CreateSparkBatchJob object itself.
     */
    public CreateSparkBatchJob withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get priority of the spark batch job in the account queue. Default is 1000, same as Scope jobs.
     *
     * @return the priority value
     */
    public Integer priority() {
        return this.priority;
    }

    /**
     * Set priority of the spark batch job in the account queue. Default is 1000, same as Scope jobs.
     *
     * @param priority the priority value to set
     * @return the CreateSparkBatchJob object itself.
     */
    public CreateSparkBatchJob withPriority(Integer priority) {
        this.priority = priority;
        return this;
    }

    /**
     * Get pipeline and recurrence parameters.
     *
     * @return the related value
     */
    public JobRelationshipParameters related() {
        return this.related;
    }

    /**
     * Set pipeline and recurrence parameters.
     *
     * @param related the related value to set
     * @return the CreateSparkBatchJob object itself.
     */
    public CreateSparkBatchJob withRelated(JobRelationshipParameters related) {
        this.related = related;
        return this;
    }

    /**
     * Get the spark batch job specific properties.
     *
     * @return the properties value
     */
    public CreateSparkBatchJobParameters properties() {
        return this.properties;
    }

    /**
     * Set the spark batch job specific properties.
     *
     * @param properties the properties value to set
     * @return the CreateSparkBatchJob object itself.
     */
    public CreateSparkBatchJob withProperties(CreateSparkBatchJobParameters properties) {
        this.properties = properties;
        return this;
    }

}
