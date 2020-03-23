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

import java.util.Map;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.rest.serializer.JsonFlatten;

/**
 * The parameters that can be used to update an existing Data Lake Analytics
 * account.
 */
@JsonFlatten
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateDataLakeAnalyticsAccountParameters {
    /**
     * The resource tags.
     */
    @JsonProperty(value = "tags")
    private Map<String, String> tags;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Properties {
        /**
         * The list of Data Lake Store accounts associated with this account.
         */
        @JsonProperty(value = "dataLakeStoreAccounts")
        private List<UpdateDataLakeStoreWithAccountParameters> dataLakeStoreAccounts;

        /**
         * The list of Azure Blob storage accounts associated with this account.
         */
        @JsonProperty(value = "storageAccounts")
        private List<UpdateStorageAccountWithAccountParameters> storageAccounts;

        /**
         * The list of compute policies associated with this account.
         */
        @JsonProperty(value = "computePolicies")
        private List<UpdateComputePolicyWithAccountParameters> computePolicies;

        /**
         * The list of firewall rules associated with this Data Lake Analytics
         * account.
         */
        @JsonProperty(value = "firewallRules")
        private List<UpdateFirewallRuleWithAccountParameters> firewallRules;

        /**
         * The current state of the IP address firewall for this Data Lake
         * Analytics account. Possible values include: 'Enabled', 'Disabled'.
         */
        @JsonProperty(value = "firewallState")
        private FirewallState firewallState;

        /**
         * The current state of allowing or disallowing IPs originating within
         * Azure through the firewall. If the firewall is disabled, this is not
         * enforced. Possible values include: 'Enabled', 'Disabled'.
         */
        @JsonProperty(value = "firewallAllowAzureIps")
        private FirewallAllowAzureIpsState firewallAllowAzureIps;

        /**
         * The commitment tier to use for next month. Possible values include:
         * 'Consumption', 'Commitment_100AUHours', 'Commitment_500AUHours',
         * 'Commitment_1000AUHours', 'Commitment_5000AUHours',
         * 'Commitment_10000AUHours', 'Commitment_50000AUHours',
         * 'Commitment_100000AUHours', 'Commitment_500000AUHours'.
         */
        @JsonProperty(value = "newTier")
        private TierType newTier;

        /**
         * The maximum supported jobs running under the account at the same time.
         */
        @JsonProperty(value = "maxJobCount")
        private Integer maxJobCount;

        /**
         * The maximum supported degree of parallelism for this account.
         */
        @JsonProperty(value = "maxDegreeOfParallelism")
        private Integer maxDegreeOfParallelism;

        /**
         * The maximum supported degree of parallelism per job for this account.
         */
        @JsonProperty(value = "maxDegreeOfParallelismPerJob")
        private Integer maxDegreeOfParallelismPerJob;

        /**
         * The minimum supported priority per job for this account.
         */
        @JsonProperty(value = "minPriorityPerJob")
        private Integer minPriorityPerJob;

        /**
         * The number of days that job metadata is retained.
         */
        @JsonProperty(value = "queryStoreRetention")
        private Integer queryStoreRetention;
    }

    /**
     * The properties
     */
    @JsonProperty(value = "properties")
    private Properties properties;

    /**
     * Get the tags value.
     *
     * @return the tags value
     */
    public Map<String, String> tags() {
        return this.tags;
    }

    /**
     * Set the tags value.
     *
     * @param tags the tags value to set
     * @return the UpdateDataLakeAnalyticsAccountParameters object itself.
     */
    public UpdateDataLakeAnalyticsAccountParameters withTags(Map<String, String> tags) {
        this.tags = tags;
        return this;
    }

    /**
     * Get the dataLakeStoreAccounts value.
     *
     * @return the dataLakeStoreAccounts value
     */
    public List<UpdateDataLakeStoreWithAccountParameters> dataLakeStoreAccounts() {
        return this.properties == null ? null : properties.dataLakeStoreAccounts;
    }

    /**
     * Set the dataLakeStoreAccounts value.
     *
     * @param dataLakeStoreAccounts the dataLakeStoreAccounts value to set
     * @return the UpdateDataLakeAnalyticsAccountParameters object itself.
     */
    public UpdateDataLakeAnalyticsAccountParameters withDataLakeStoreAccounts(List<UpdateDataLakeStoreWithAccountParameters> dataLakeStoreAccounts) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.dataLakeStoreAccounts = dataLakeStoreAccounts;
        return this;
    }

    /**
     * Get the storageAccounts value.
     *
     * @return the storageAccounts value
     */
    public List<UpdateStorageAccountWithAccountParameters> storageAccounts() {
        return this.properties == null ? null : properties.storageAccounts;
    }

    /**
     * Set the storageAccounts value.
     *
     * @param storageAccounts the storageAccounts value to set
     * @return the UpdateDataLakeAnalyticsAccountParameters object itself.
     */
    public UpdateDataLakeAnalyticsAccountParameters withStorageAccounts(List<UpdateStorageAccountWithAccountParameters> storageAccounts) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.storageAccounts = storageAccounts;
        return this;
    }

    /**
     * Get the computePolicies value.
     *
     * @return the computePolicies value
     */
    public List<UpdateComputePolicyWithAccountParameters> computePolicies() {
        return this.properties == null ? null : properties.computePolicies;
    }

    /**
     * Set the computePolicies value.
     *
     * @param computePolicies the computePolicies value to set
     * @return the UpdateDataLakeAnalyticsAccountParameters object itself.
     */
    public UpdateDataLakeAnalyticsAccountParameters withComputePolicies(List<UpdateComputePolicyWithAccountParameters> computePolicies) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.computePolicies = computePolicies;
        return this;
    }

    /**
     * Get the firewallRules value.
     *
     * @return the firewallRules value
     */
    public List<UpdateFirewallRuleWithAccountParameters> firewallRules() {
        return this.properties == null ? null : properties.firewallRules;
    }

    /**
     * Set the firewallRules value.
     *
     * @param firewallRules the firewallRules value to set
     * @return the UpdateDataLakeAnalyticsAccountParameters object itself.
     */
    public UpdateDataLakeAnalyticsAccountParameters withFirewallRules(List<UpdateFirewallRuleWithAccountParameters> firewallRules) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.firewallRules = firewallRules;
        return this;
    }

    /**
     * Get the firewallState value.
     *
     * @return the firewallState value
     */
    public FirewallState firewallState() {
        return this.properties == null ? null : properties.firewallState;
    }

    /**
     * Set the firewallState value.
     *
     * @param firewallState the firewallState value to set
     * @return the UpdateDataLakeAnalyticsAccountParameters object itself.
     */
    public UpdateDataLakeAnalyticsAccountParameters withFirewallState(FirewallState firewallState) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.firewallState = firewallState;
        return this;
    }

    /**
     * Get the firewallAllowAzureIps value.
     *
     * @return the firewallAllowAzureIps value
     */
    public FirewallAllowAzureIpsState firewallAllowAzureIps() {
        return this.properties == null ? null : properties.firewallAllowAzureIps;
    }

    /**
     * Set the firewallAllowAzureIps value.
     *
     * @param firewallAllowAzureIps the firewallAllowAzureIps value to set
     * @return the UpdateDataLakeAnalyticsAccountParameters object itself.
     */
    public UpdateDataLakeAnalyticsAccountParameters withFirewallAllowAzureIps(FirewallAllowAzureIpsState firewallAllowAzureIps) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.firewallAllowAzureIps = firewallAllowAzureIps;
        return this;
    }

    /**
     * Get the newTier value.
     *
     * @return the newTier value
     */
    public TierType newTier() {
        return this.properties == null ? null : properties.newTier;
    }

    /**
     * Set the newTier value.
     *
     * @param newTier the newTier value to set
     * @return the UpdateDataLakeAnalyticsAccountParameters object itself.
     */
    public UpdateDataLakeAnalyticsAccountParameters withNewTier(TierType newTier) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.newTier = newTier;
        return this;
    }

    /**
     * Get the maxJobCount value.
     *
     * @return the maxJobCount value
     */
    public Integer maxJobCount() {
        return this.properties == null ? null : properties.maxJobCount;
    }

    /**
     * Set the maxJobCount value.
     *
     * @param maxJobCount the maxJobCount value to set
     * @return the UpdateDataLakeAnalyticsAccountParameters object itself.
     */
    public UpdateDataLakeAnalyticsAccountParameters withMaxJobCount(Integer maxJobCount) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.maxJobCount = maxJobCount;
        return this;
    }

    /**
     * Get the maxDegreeOfParallelism value.
     *
     * @return the maxDegreeOfParallelism value
     */
    public Integer maxDegreeOfParallelism() {
        return this.properties == null ? null : properties.maxDegreeOfParallelism;
    }

    /**
     * Set the maxDegreeOfParallelism value.
     *
     * @param maxDegreeOfParallelism the maxDegreeOfParallelism value to set
     * @return the UpdateDataLakeAnalyticsAccountParameters object itself.
     */
    public UpdateDataLakeAnalyticsAccountParameters withMaxDegreeOfParallelism(Integer maxDegreeOfParallelism) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.maxDegreeOfParallelism = maxDegreeOfParallelism;
        return this;
    }

    /**
     * Get the maxDegreeOfParallelismPerJob value.
     *
     * @return the maxDegreeOfParallelismPerJob value
     */
    public Integer maxDegreeOfParallelismPerJob() {
        return this.properties == null ? null : properties.maxDegreeOfParallelismPerJob;
    }

    /**
     * Set the maxDegreeOfParallelismPerJob value.
     *
     * @param maxDegreeOfParallelismPerJob the maxDegreeOfParallelismPerJob value to set
     * @return the UpdateDataLakeAnalyticsAccountParameters object itself.
     */
    public UpdateDataLakeAnalyticsAccountParameters withMaxDegreeOfParallelismPerJob(Integer maxDegreeOfParallelismPerJob) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.maxDegreeOfParallelismPerJob = maxDegreeOfParallelismPerJob;
        return this;
    }

    /**
     * Get the minPriorityPerJob value.
     *
     * @return the minPriorityPerJob value
     */
    public Integer minPriorityPerJob() {
        return this.properties == null ? null : properties.minPriorityPerJob;
    }

    /**
     * Set the minPriorityPerJob value.
     *
     * @param minPriorityPerJob the minPriorityPerJob value to set
     * @return the UpdateDataLakeAnalyticsAccountParameters object itself.
     */
    public UpdateDataLakeAnalyticsAccountParameters withMinPriorityPerJob(Integer minPriorityPerJob) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.minPriorityPerJob = minPriorityPerJob;
        return this;
    }

    /**
     * Get the queryStoreRetention value.
     *
     * @return the queryStoreRetention value
     */
    public Integer queryStoreRetention() {
        return this.properties == null ? null : properties.queryStoreRetention;
    }

    /**
     * Set the queryStoreRetention value.
     *
     * @param queryStoreRetention the queryStoreRetention value to set
     * @return the UpdateDataLakeAnalyticsAccountParameters object itself.
     */
    public UpdateDataLakeAnalyticsAccountParameters withQueryStoreRetention(Integer queryStoreRetention) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.queryStoreRetention = queryStoreRetention;
        return this;
    }

}
