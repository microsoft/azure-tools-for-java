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
package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.api.activityTypes.spark.resourcePools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.CreateSparkResourcePool;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

/**
 * Submits a resource pool creation request to the specified Data Lake Analytics account
 */
public class PutResourcePoolId implements IConvertible {
    /**
     * The parameters to submit a spark resource pool creation request
     */
    @NotNull
    @JsonProperty(value = "parameters", required = true)
    private CreateSparkResourcePool parameters;

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
    public PutResourcePoolId withParameters(@NotNull CreateSparkResourcePool parameters) {
        this.parameters = parameters;
        return this;
    }
}
