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

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models;

import java.util.Date;
import java.util.UUID;
import java.util.List;
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
public class DataLakeAnalyticsAccount extends Resource {
    /**
     * The unique identifier associated with this Data Lake Analytics account.
     */
    @JsonProperty(value = "properties.accountId", access = JsonProperty.Access.WRITE_ONLY)
    private UUID accountId;

    /**
     * The provisioning status of the Data Lake Analytics account. Possible
     * values include: 'Failed', 'Creating', 'Running', 'Succeeded',
     * 'Patching', 'Suspending', 'Resuming', 'Deleting', 'Deleted',
     * 'Undeleting', 'Canceled'.
     */
    @JsonProperty(value = "properties.provisioningState", access = JsonProperty.Access.WRITE_ONLY)
    private DataLakeAnalyticsAccountStatus provisioningState;

    /**
     * The state of the Data Lake Analytics account. Possible values include:
     * 'Active', 'Suspended'.
     */
    @JsonProperty(value = "properties.state", access = JsonProperty.Access.WRITE_ONLY)
    private DataLakeAnalyticsAccountState state;

    /**
     * The account creation time.
     */
    @JsonProperty(value = "properties.creationTime", access = JsonProperty.Access.WRITE_ONLY)
    private Date creationTime;

    /**
     * The account last modified time.
     */
    @JsonProperty(value = "properties.lastModifiedTime", access = JsonProperty.Access.WRITE_ONLY)
    private Date lastModifiedTime;

    /**
     * The full CName endpoint for this account.
     */
    @JsonProperty(value = "properties.endpoint", access = JsonProperty.Access.WRITE_ONLY)
    private String endpoint;

    /**
     * The default Data Lake Store account associated with this account.
     */
    @JsonProperty(value = "properties.defaultDataLakeStoreAccount", access = JsonProperty.Access.WRITE_ONLY)
    private String defaultDataLakeStoreAccount;

    /**
     * The list of Data Lake Store accounts associated with this account.
     */
    @JsonProperty(value = "properties.dataLakeStoreAccounts", access = JsonProperty.Access.WRITE_ONLY)
    private List<DataLakeStoreAccountInformation> dataLakeStoreAccounts;

    /**
     * The list of Azure Blob Storage accounts associated with this account.
     */
    @JsonProperty(value = "properties.storageAccounts", access = JsonProperty.Access.WRITE_ONLY)
    private List<StorageAccountInformation> storageAccounts;

    /**
     * The list of compute policies associated with this account.
     */
    @JsonProperty(value = "properties.computePolicies", access = JsonProperty.Access.WRITE_ONLY)
    private List<ComputePolicy> computePolicies;

    /**
     * The list of firewall rules associated with this account.
     */
    @JsonProperty(value = "properties.firewallRules", access = JsonProperty.Access.WRITE_ONLY)
    private List<FirewallRule> firewallRules;

    /**
     * The current state of the IP address firewall for this account. Possible
     * values include: 'Enabled', 'Disabled'.
     */
    @JsonProperty(value = "properties.firewallState", access = JsonProperty.Access.WRITE_ONLY)
    private FirewallState firewallState;

    /**
     * The current state of allowing or disallowing IPs originating within
     * Azure through the firewall. If the firewall is disabled, this is not
     * enforced. Possible values include: 'Enabled', 'Disabled'.
     */
    @JsonProperty(value = "properties.firewallAllowAzureIps", access = JsonProperty.Access.WRITE_ONLY)
    private FirewallAllowAzureIpsState firewallAllowAzureIps;

    /**
     * The commitment tier for the next month. Possible values include:
     * 'Consumption', 'Commitment_100AUHours', 'Commitment_500AUHours',
     * 'Commitment_1000AUHours', 'Commitment_5000AUHours',
     * 'Commitment_10000AUHours', 'Commitment_50000AUHours',
     * 'Commitment_100000AUHours', 'Commitment_500000AUHours'.
     */
    @JsonProperty(value = "properties.newTier", access = JsonProperty.Access.WRITE_ONLY)
    private TierType newTier;

    /**
     * The commitment tier in use for the current month. Possible values
     * include: 'Consumption', 'Commitment_100AUHours',
     * 'Commitment_500AUHours', 'Commitment_1000AUHours',
     * 'Commitment_5000AUHours', 'Commitment_10000AUHours',
     * 'Commitment_50000AUHours', 'Commitment_100000AUHours',
     * 'Commitment_500000AUHours'.
     */
    @JsonProperty(value = "properties.currentTier", access = JsonProperty.Access.WRITE_ONLY)
    private TierType currentTier;

    /**
     * The maximum supported jobs running under the account at the same time.
     */
    @JsonProperty(value = "properties.maxJobCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer maxJobCount;

    /**
     * The system defined maximum supported jobs running under the account at
     * the same time, which restricts the maximum number of running jobs the
     * user can set for the account.
     */
    @JsonProperty(value = "properties.systemMaxJobCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer systemMaxJobCount;

    /**
     * The maximum supported degree of parallelism for this account.
     */
    @JsonProperty(value = "properties.maxDegreeOfParallelism", access = JsonProperty.Access.WRITE_ONLY)
    private Integer maxDegreeOfParallelism;

    /**
     * The system defined maximum supported degree of parallelism for this
     * account, which restricts the maximum value of parallelism the user can
     * set for the account.
     */
    @JsonProperty(value = "properties.systemMaxDegreeOfParallelism", access = JsonProperty.Access.WRITE_ONLY)
    private Integer systemMaxDegreeOfParallelism;

    /**
     * The maximum supported degree of parallelism per job for this account.
     */
    @JsonProperty(value = "properties.maxDegreeOfParallelismPerJob", access = JsonProperty.Access.WRITE_ONLY)
    private Integer maxDegreeOfParallelismPerJob;

    /**
     * The minimum supported priority per job for this account.
     */
    @JsonProperty(value = "properties.minPriorityPerJob", access = JsonProperty.Access.WRITE_ONLY)
    private Integer minPriorityPerJob;

    /**
     * The number of days that job metadata is retained.
     */
    @JsonProperty(value = "properties.queryStoreRetention", access = JsonProperty.Access.WRITE_ONLY)
    private Integer queryStoreRetention;

    /**
     * Get the accountId value.
     *
     * @return the accountId value
     */
    public UUID accountId() {
        return this.accountId;
    }

    /**
     * Get the provisioningState value.
     *
     * @return the provisioningState value
     */
    public DataLakeAnalyticsAccountStatus provisioningState() {
        return this.provisioningState;
    }

    /**
     * Get the state value.
     *
     * @return the state value
     */
    public DataLakeAnalyticsAccountState state() {
        return this.state;
    }

    /**
     * Get the creationTime value.
     *
     * @return the creationTime value
     */
    public Date creationTime() {
        return this.creationTime;
    }

    /**
     * Get the lastModifiedTime value.
     *
     * @return the lastModifiedTime value
     */
    public Date lastModifiedTime() {
        return this.lastModifiedTime;
    }

    /**
     * Get the endpoint value.
     *
     * @return the endpoint value
     */
    public String endpoint() {
        return this.endpoint;
    }

    /**
     * Get the defaultDataLakeStoreAccount value.
     *
     * @return the defaultDataLakeStoreAccount value
     */
    public String defaultDataLakeStoreAccount() {
        return this.defaultDataLakeStoreAccount;
    }

    /**
     * Get the dataLakeStoreAccounts value.
     *
     * @return the dataLakeStoreAccounts value
     */
    public List<DataLakeStoreAccountInformation> dataLakeStoreAccounts() {
        return this.dataLakeStoreAccounts;
    }

    /**
     * Get the storageAccounts value.
     *
     * @return the storageAccounts value
     */
    public List<StorageAccountInformation> storageAccounts() {
        return this.storageAccounts;
    }

    /**
     * Get the computePolicies value.
     *
     * @return the computePolicies value
     */
    public List<ComputePolicy> computePolicies() {
        return this.computePolicies;
    }

    /**
     * Get the firewallRules value.
     *
     * @return the firewallRules value
     */
    public List<FirewallRule> firewallRules() {
        return this.firewallRules;
    }

    /**
     * Get the firewallState value.
     *
     * @return the firewallState value
     */
    public FirewallState firewallState() {
        return this.firewallState;
    }

    /**
     * Get the firewallAllowAzureIps value.
     *
     * @return the firewallAllowAzureIps value
     */
    public FirewallAllowAzureIpsState firewallAllowAzureIps() {
        return this.firewallAllowAzureIps;
    }

    /**
     * Get the newTier value.
     *
     * @return the newTier value
     */
    public TierType newTier() {
        return this.newTier;
    }

    /**
     * Get the currentTier value.
     *
     * @return the currentTier value
     */
    public TierType currentTier() {
        return this.currentTier;
    }

    /**
     * Get the maxJobCount value.
     *
     * @return the maxJobCount value
     */
    public Integer maxJobCount() {
        return this.maxJobCount;
    }

    /**
     * Get the systemMaxJobCount value.
     *
     * @return the systemMaxJobCount value
     */
    public Integer systemMaxJobCount() {
        return this.systemMaxJobCount;
    }

    /**
     * Get the maxDegreeOfParallelism value.
     *
     * @return the maxDegreeOfParallelism value
     */
    public Integer maxDegreeOfParallelism() {
        return this.maxDegreeOfParallelism;
    }

    /**
     * Get the systemMaxDegreeOfParallelism value.
     *
     * @return the systemMaxDegreeOfParallelism value
     */
    public Integer systemMaxDegreeOfParallelism() {
        return this.systemMaxDegreeOfParallelism;
    }

    /**
     * Get the maxDegreeOfParallelismPerJob value.
     *
     * @return the maxDegreeOfParallelismPerJob value
     */
    public Integer maxDegreeOfParallelismPerJob() {
        return this.maxDegreeOfParallelismPerJob;
    }

    /**
     * Get the minPriorityPerJob value.
     *
     * @return the minPriorityPerJob value
     */
    public Integer minPriorityPerJob() {
        return this.minPriorityPerJob;
    }

    /**
     * Get the queryStoreRetention value.
     *
     * @return the queryStoreRetention value
     */
    public Integer queryStoreRetention() {
        return this.queryStoreRetention;
    }

}
