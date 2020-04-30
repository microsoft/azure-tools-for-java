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

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models;

import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.rest.serializer.JsonFlatten;
import com.microsoft.rest.SkipParentValidation;
import com.microsoft.azure.Resource;

/**
 * A Data Lake Analytics account object, containing all information associated
 * with the named Data Lake Analytics account.
 */
@JsonFlatten
@SkipParentValidation
//@JsonIgnoreProperties(value = "properties", ignoreUnknown = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataLakeAnalyticsAccountBasic extends Resource {
    public static class Properties {
        /**
         * The unique identifier associated with this Data Lake Analytics account.
         */
        @JsonProperty(value = "accountId", access = JsonProperty.Access.WRITE_ONLY)
        private UUID accountId;

        /**
         * The provisioning status of the Data Lake Analytics account. Possible
         * values include: 'Failed', 'Creating', 'Running', 'Succeeded',
         * 'Patching', 'Suspending', 'Resuming', 'Deleting', 'Deleted',
         * 'Undeleting', 'Canceled'.
         */
        @JsonProperty(value = "provisioningState", access = JsonProperty.Access.WRITE_ONLY)
        private DataLakeAnalyticsAccountStatus provisioningState;

        /**
         * The state of the Data Lake Analytics account. Possible values include:
         * 'Active', 'Suspended'.
         */
        @JsonProperty(value = "state", access = JsonProperty.Access.WRITE_ONLY)
        private DataLakeAnalyticsAccountState state;

        /**
         * The account creation time.
         */
        @JsonProperty(value = "creationTime", access = JsonProperty.Access.WRITE_ONLY)
        private Date creationTime;

        /**
         * The account last modified time.
         */
        @JsonProperty(value = "lastModifiedTime", access = JsonProperty.Access.WRITE_ONLY)
        private Date lastModifiedTime;

        /**
         * The full CName endpoint for this account.
         */
        @JsonProperty(value = "endpoint", access = JsonProperty.Access.WRITE_ONLY)
        private String endpoint;
    }

    @JsonProperty(value = "properties", access = JsonProperty.Access.WRITE_ONLY)
    private Properties properties;

    /**
     * Get the accountId value.
     *
     * @return the accountId value
     */
    public UUID accountId() {
        return properties != null ? this.properties.accountId : null;
    }

    /**
     * Get the provisioningState value.
     *
     * @return the provisioningState value
     */
    public DataLakeAnalyticsAccountStatus provisioningState() {
        return properties != null ? this.properties.provisioningState : null;
    }

    /**
     * Get the state value.
     *
     * @return the state value
     */
    public DataLakeAnalyticsAccountState state() {
        return properties != null ? this.properties.state : null;
    }

    /**
     * Get the creationTime value.
     *
     * @return the creationTime value
     */
    public Date creationTime() {
        return properties != null ? this.properties.creationTime : null;
    }

    /**
     * Get the lastModifiedTime value.
     *
     * @return the lastModifiedTime value
     */
    public Date lastModifiedTime() {
        return properties != null ? this.properties.lastModifiedTime : null;
    }

    /**
     * Get the endpoint value.
     *
     * @return the endpoint value
     */
    public String endpoint() {
        return properties != null ? this.properties.endpoint : null;
    }

}
