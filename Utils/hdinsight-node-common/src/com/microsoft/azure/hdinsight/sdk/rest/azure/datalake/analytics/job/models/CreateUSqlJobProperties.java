/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * U-SQL job properties used when submitting U-SQL jobs.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeName("USql")
public class CreateUSqlJobProperties extends CreateJobProperties {
    /**
     * The specific compilation mode for the job used during execution. If this is not specified during submission, the
     * server will determine the optimal compilation mode. Possible values include: 'Semantic', 'Full', 'SingleBox'.
     */
    @JsonProperty(value = "compileMode")
    private CompileMode compileMode;

    /**
     * Get the specific compilation mode for the job used during execution. If this is not specified during submission, the server will determine the optimal compilation mode. Possible values include: 'Semantic', 'Full', 'SingleBox'.
     *
     * @return the compileMode value
     */
    public CompileMode compileMode() {
        return this.compileMode;
    }

    /**
     * Set the specific compilation mode for the job used during execution. If this is not specified during submission, the server will determine the optimal compilation mode. Possible values include: 'Semantic', 'Full', 'SingleBox'.
     *
     * @param compileMode the compileMode value to set
     * @return the CreateUSqlJobProperties object itself.
     */
    public CreateUSqlJobProperties withCompileMode(CompileMode compileMode) {
        this.compileMode = compileMode;
        return this;
    }

}
