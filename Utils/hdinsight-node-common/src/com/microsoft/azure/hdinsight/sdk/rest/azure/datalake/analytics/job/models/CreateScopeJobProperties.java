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

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Scope job properties used when submitting Scope jobs. (Only for use internally with Scope job type.).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeName("Scope")
public class CreateScopeJobProperties extends CreateJobProperties {
    /**
     * The list of resources that are required by the job.
     */
    @JsonProperty(value = "resources")
    private List<ScopeJobResource> resources;

    /**
     * The list of email addresses, separated by semi-colons, to notify when the job reaches a terminal state.
     */
    @JsonProperty(value = "notifier")
    private String notifier;

    /**
     * Get the list of resources that are required by the job.
     *
     * @return the resources value
     */
    public List<ScopeJobResource> resources() {
        return this.resources;
    }

    /**
     * Set the list of resources that are required by the job.
     *
     * @param resources the resources value to set
     * @return the CreateScopeJobProperties object itself.
     */
    public CreateScopeJobProperties withResources(List<ScopeJobResource> resources) {
        this.resources = resources;
        return this;
    }

    /**
     * Get the list of email addresses, separated by semi-colons, to notify when the job reaches a terminal state.
     *
     * @return the notifier value
     */
    public String notifier() {
        return this.notifier;
    }

    /**
     * Set the list of email addresses, separated by semi-colons, to notify when the job reaches a terminal state.
     *
     * @param notifier the notifier value to set
     * @return the CreateScopeJobProperties object itself.
     */
    public CreateScopeJobProperties withNotifier(String notifier) {
        this.notifier = notifier;
        return this;
    }

}
