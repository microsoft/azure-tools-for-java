/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * List of JobInfo items.
 */
public class JobInfoListResult {
    /**
     * the list of JobInfo items..
     */
    @JsonProperty(value = "value", access = JsonProperty.Access.WRITE_ONLY)
    private List<JobInformation> value;
    /**
     * the link (url) to the next page of results.
     */
    @JsonProperty(value = "nextLink", access = JsonProperty.Access.WRITE_ONLY)
    private String nextLink;
    /**
     * the count of jobs in the result.
     */
    @JsonProperty(value = "count", access = JsonProperty.Access.WRITE_ONLY)
    private String count;

    /**
     * Get the properties.
     * @return the properties value
     */
    public List<JobInformation> value() {
        return this.value;
    }

    /**
     * Get the next Link
     * @return the nextLink value
     */
    public String nextLink() {
        return this.nextLink;
    }

    /**
     * Get the count
     * @return the count value
     */
    public String count() {
        return this.count;
    }
}
