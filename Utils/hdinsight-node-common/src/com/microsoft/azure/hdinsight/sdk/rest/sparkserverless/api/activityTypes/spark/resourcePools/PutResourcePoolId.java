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

package com.microsoft.azure.hdinsight.sdk.rest.sparkserverless.api.activityTypes.spark.resourcePools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.hdinsight.sdk.rest.sparkserverless.CreateSparkResourcePool;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

/**
 * Submits a resource pool creation request to the specified Data Lake Analytics account
 */
public class PutResourcePoolId {
    /**
     * resource pool identifier.
     * Uniquely identifies the resource pool across all resource pools submitted to the service
     */
    @NotNull
    @JsonProperty(value = "resourcePoolId", required = true)
    private String resourcePoolId;

    /**
     * The parameters to submit a spark resource pool creation request
     */
    @NotNull
    @JsonProperty(value = "parameters", required = true)
    private CreateSparkResourcePool parameters;

    // TODO: maybe one more filed: ApiVersionParameter?

    /**
     * get the resourcePoolId value
     * @return the resourcePoolId value
     */
    @NotNull
    public String getResourcePoolId() {
        return resourcePoolId;
    }

    /**
     * set the resourcePoolId value
     * @param resourcePoolId the resourcePoolId value to set
     */
    public void setResourcePoolId(@NotNull String resourcePoolId) {
        this.resourcePoolId = resourcePoolId;
    }

    /**
     * get the parameters value
     * @return the parameters value
     */
    @NotNull
    public CreateSparkResourcePool getParameters() {
        return parameters;
    }

    /**
     * set the parameters value
     * @param parameters the parameters value to set
     */
    public void setParameters(@NotNull CreateSparkResourcePool parameters) {
        this.parameters = parameters;
    }
}
