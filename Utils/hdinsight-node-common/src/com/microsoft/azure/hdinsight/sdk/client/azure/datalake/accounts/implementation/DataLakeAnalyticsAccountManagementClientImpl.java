/**
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
 *
 */

package com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.implementation;

import com.microsoft.azure.AzureClient;
import com.microsoft.azure.AzureServiceClient;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.Accounts;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.ComputePolicies;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.DataLakeAnalyticsAccountManagementClient;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.DataLakeStoreAccounts;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.FirewallRules;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.Locations;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.Operations;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.StorageAccounts;
import com.microsoft.rest.credentials.ServiceClientCredentials;
import com.microsoft.rest.RestClient;

/**
 * Initializes a new instance of the DataLakeAnalyticsAccountManagementClientImpl class.
 */
public class DataLakeAnalyticsAccountManagementClientImpl extends AzureServiceClient implements DataLakeAnalyticsAccountManagementClient {
    /** the {@link AzureClient} used for long running operations. */
    private AzureClient azureClient;

    /**
     * Gets the {@link AzureClient} used for long running operations.
     * @return the azure client;
     */
    public AzureClient getAzureClient() {
        return this.azureClient;
    }

    /** Get subscription credentials which uniquely identify Microsoft Azure subscription. The subscription ID forms part of the URI for every service call. */
    private String subscriptionId;

    /**
     * Gets Get subscription credentials which uniquely identify Microsoft Azure subscription. The subscription ID forms part of the URI for every service call.
     *
     * @return the subscriptionId value.
     */
    public String subscriptionId() {
        return this.subscriptionId;
    }

    /**
     * Sets Get subscription credentials which uniquely identify Microsoft Azure subscription. The subscription ID forms part of the URI for every service call.
     *
     * @param subscriptionId the subscriptionId value.
     * @return the service client itself
     */
    public DataLakeAnalyticsAccountManagementClientImpl withSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
        return this;
    }

    /** Client Api Version. */
    private String apiVersion;

    /**
     * Gets Client Api Version.
     *
     * @return the apiVersion value.
     */
    public String apiVersion() {
        return this.apiVersion;
    }

    /** Gets or sets the preferred language for the response. */
    private String acceptLanguage;

    /**
     * Gets Gets or sets the preferred language for the response.
     *
     * @return the acceptLanguage value.
     */
    public String acceptLanguage() {
        return this.acceptLanguage;
    }

    /**
     * Sets Gets or sets the preferred language for the response.
     *
     * @param acceptLanguage the acceptLanguage value.
     * @return the service client itself
     */
    public DataLakeAnalyticsAccountManagementClientImpl withAcceptLanguage(String acceptLanguage) {
        this.acceptLanguage = acceptLanguage;
        return this;
    }

    /** Gets or sets the retry timeout in seconds for Long Running Operations. Default value is 30. */
    private int longRunningOperationRetryTimeout;

    /**
     * Gets Gets or sets the retry timeout in seconds for Long Running Operations. Default value is 30.
     *
     * @return the longRunningOperationRetryTimeout value.
     */
    public int longRunningOperationRetryTimeout() {
        return this.longRunningOperationRetryTimeout;
    }

    /**
     * Sets Gets or sets the retry timeout in seconds for Long Running Operations. Default value is 30.
     *
     * @param longRunningOperationRetryTimeout the longRunningOperationRetryTimeout value.
     * @return the service client itself
     */
    public DataLakeAnalyticsAccountManagementClientImpl withLongRunningOperationRetryTimeout(int longRunningOperationRetryTimeout) {
        this.longRunningOperationRetryTimeout = longRunningOperationRetryTimeout;
        return this;
    }

    /** When set to true a unique x-ms-client-request-id value is generated and included in each request. Default is true. */
    private boolean generateClientRequestId;

    /**
     * Gets When set to true a unique x-ms-client-request-id value is generated and included in each request. Default is true.
     *
     * @return the generateClientRequestId value.
     */
    public boolean generateClientRequestId() {
        return this.generateClientRequestId;
    }

    /**
     * Sets When set to true a unique x-ms-client-request-id value is generated and included in each request. Default is true.
     *
     * @param generateClientRequestId the generateClientRequestId value.
     * @return the service client itself
     */
    public DataLakeAnalyticsAccountManagementClientImpl withGenerateClientRequestId(boolean generateClientRequestId) {
        this.generateClientRequestId = generateClientRequestId;
        return this;
    }

    /**
     * The Accounts object to access its operations.
     */
    private Accounts accounts;

    /**
     * Gets the Accounts object to access its operations.
     * @return the Accounts object.
     */
    public Accounts accounts() {
        return this.accounts;
    }

    /**
     * The DataLakeStoreAccounts object to access its operations.
     */
    private DataLakeStoreAccounts dataLakeStoreAccounts;

    /**
     * Gets the DataLakeStoreAccounts object to access its operations.
     * @return the DataLakeStoreAccounts object.
     */
    public DataLakeStoreAccounts dataLakeStoreAccounts() {
        return this.dataLakeStoreAccounts;
    }

    /**
     * The StorageAccounts object to access its operations.
     */
    private StorageAccounts storageAccounts;

    /**
     * Gets the StorageAccounts object to access its operations.
     * @return the StorageAccounts object.
     */
    public StorageAccounts storageAccounts() {
        return this.storageAccounts;
    }

    /**
     * The ComputePolicies object to access its operations.
     */
    private ComputePolicies computePolicies;

    /**
     * Gets the ComputePolicies object to access its operations.
     * @return the ComputePolicies object.
     */
    public ComputePolicies computePolicies() {
        return this.computePolicies;
    }

    /**
     * The FirewallRules object to access its operations.
     */
    private FirewallRules firewallRules;

    /**
     * Gets the FirewallRules object to access its operations.
     * @return the FirewallRules object.
     */
    public FirewallRules firewallRules() {
        return this.firewallRules;
    }

    /**
     * The Operations object to access its operations.
     */
    private Operations operations;

    /**
     * Gets the Operations object to access its operations.
     * @return the Operations object.
     */
    public Operations operations() {
        return this.operations;
    }

    /**
     * The Locations object to access its operations.
     */
    private Locations locations;

    /**
     * Gets the Locations object to access its operations.
     * @return the Locations object.
     */
    public Locations locations() {
        return this.locations;
    }

    /**
     * Initializes an instance of DataLakeAnalyticsAccountManagementClient client.
     *
     * @param credentials the management credentials for Azure
     */
    public DataLakeAnalyticsAccountManagementClientImpl(ServiceClientCredentials credentials) {
        this("https://management.azure.com", credentials);
    }

    /**
     * Initializes an instance of DataLakeAnalyticsAccountManagementClient client.
     *
     * @param baseUrl the base URL of the host
     * @param credentials the management credentials for Azure
     */
    public DataLakeAnalyticsAccountManagementClientImpl(String baseUrl, ServiceClientCredentials credentials) {
        super(baseUrl, credentials);
        initialize();
    }

    /**
     * Initializes an instance of DataLakeAnalyticsAccountManagementClient client.
     *
     * @param restClient the REST client to connect to Azure.
     */
    public DataLakeAnalyticsAccountManagementClientImpl(RestClient restClient) {
        super(restClient);
        initialize();
    }

    protected void initialize() {
        this.apiVersion = "2016-11-01";
        this.acceptLanguage = "en-US";
        this.longRunningOperationRetryTimeout = 30;
        this.generateClientRequestId = true;
        this.accounts = new AccountsImpl(restClient().retrofit(), this);
        this.dataLakeStoreAccounts = new DataLakeStoreAccountsImpl(restClient().retrofit(), this);
        this.storageAccounts = new StorageAccountsImpl(restClient().retrofit(), this);
        this.computePolicies = new ComputePoliciesImpl(restClient().retrofit(), this);
        this.firewallRules = new FirewallRulesImpl(restClient().retrofit(), this);
        this.operations = new OperationsImpl(restClient().retrofit(), this);
        this.locations = new LocationsImpl(restClient().retrofit(), this);
        this.azureClient = new AzureClient(this);
    }

    /**
     * Gets the User-Agent header for the client.
     *
     * @return the user agent string.
     */
    @Override
    public String userAgent() {
        return String.format("%s (%s, %s)", super.userAgent(), "DataLakeAnalyticsAccountManagementClient", "2016-11-01");
    }
}
