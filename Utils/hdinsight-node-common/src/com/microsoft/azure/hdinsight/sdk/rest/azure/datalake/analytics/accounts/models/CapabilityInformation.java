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

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subscription-level properties and limits for Data Lake Analytics.
 */
public class CapabilityInformation {
    /**
     * The subscription credentials that uniquely identifies the subscription.
     */
    @JsonProperty(value = "subscriptionId", access = JsonProperty.Access.WRITE_ONLY)
    private UUID subscriptionId;

    /**
     * The subscription state. Possible values include: 'Registered',
     * 'Suspended', 'Deleted', 'Unregistered', 'Warned'.
     */
    @JsonProperty(value = "state", access = JsonProperty.Access.WRITE_ONLY)
    private SubscriptionState state;

    /**
     * The maximum supported number of accounts under this subscription.
     */
    @JsonProperty(value = "maxAccountCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer maxAccountCount;

    /**
     * The current number of accounts under this subscription.
     */
    @JsonProperty(value = "accountCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer accountCount;

    /**
     * The Boolean value of true or false to indicate the maintenance state.
     */
    @JsonProperty(value = "migrationState", access = JsonProperty.Access.WRITE_ONLY)
    private Boolean migrationState;

    /**
     * Get the subscriptionId value.
     *
     * @return the subscriptionId value
     */
    public UUID subscriptionId() {
        return this.subscriptionId;
    }

    /**
     * Get the state value.
     *
     * @return the state value
     */
    public SubscriptionState state() {
        return this.state;
    }

    /**
     * Get the maxAccountCount value.
     *
     * @return the maxAccountCount value
     */
    public Integer maxAccountCount() {
        return this.maxAccountCount;
    }

    /**
     * Get the accountCount value.
     *
     * @return the accountCount value
     */
    public Integer accountCount() {
        return this.accountCount;
    }

    /**
     * Get the migrationState value.
     *
     * @return the migrationState value
     */
    public Boolean migrationState() {
        return this.migrationState;
    }

}
