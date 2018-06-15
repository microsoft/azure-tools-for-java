/**
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
package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models.JobInfoListResult;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.Map;

/**
 * Lists the jobs, if any, associated with the specified Data Lake Analytics account.
 * The response includes a link to the next page of results, if any.
 */
public class GetJobListResponse {
    public static final Map<Integer, String> successfulResponses = ImmutableMap.of(
            200, "Successfully retrieved the list of jobs.");

    @NotNull
    @JsonProperty(value = "sparkResourcePoolList")
    private JobInfoListResult jobInfoListResult;

    /**
     * Get the list of job information
     * @return the jobInfoListResult value
     */
    @NotNull
    public JobInfoListResult jobInfoListResult() {
        return this.jobInfoListResult;
    }
}
