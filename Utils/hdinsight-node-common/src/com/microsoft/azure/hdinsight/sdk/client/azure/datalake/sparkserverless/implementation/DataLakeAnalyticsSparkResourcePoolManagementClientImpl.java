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

package com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.implementation;

import com.microsoft.azure.AzureClient;
import com.microsoft.azure.AzureServiceClient;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.DataLakeAnalyticsSparkResourcePoolManagementClient;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.SparkResourcePools;
import com.microsoft.rest.credentials.ServiceClientCredentials;
import com.microsoft.rest.RestClient;

/**
 * Initializes a new instance of the DataLakeAnalyticsSparkResourcePoolManagementClientImpl class.
 */
public class DataLakeAnalyticsSparkResourcePoolManagementClientImpl extends AzureServiceClient implements DataLakeAnalyticsSparkResourcePoolManagementClient {
    /** the {@link AzureClient} used for long running operations. */
    private AzureClient azureClient;

    /**
     * Gets the {@link AzureClient} used for long running operations.
     * @return the azure client;
     */
    public AzureClient getAzureClient() {
        return this.azureClient;
    }

    /** The DNS suffix used as the base for all Azure Data Lake Analytics resource pool service requests. */
    private String adlaResourcePoolDnsSuffix;

    /**
     * Gets The DNS suffix used as the base for all Azure Data Lake Analytics resource pool service requests.
     *
     * @return the adlaResourcePoolDnsSuffix value.
     */
    public String adlaResourcePoolDnsSuffix() {
        return this.adlaResourcePoolDnsSuffix;
    }

    /**
     * Sets The DNS suffix used as the base for all Azure Data Lake Analytics resource pool service requests.
     *
     * @param adlaResourcePoolDnsSuffix the adlaResourcePoolDnsSuffix value.
     * @return the service client itself
     */
    public DataLakeAnalyticsSparkResourcePoolManagementClientImpl withAdlaResourcePoolDnsSuffix(String adlaResourcePoolDnsSuffix) {
        this.adlaResourcePoolDnsSuffix = adlaResourcePoolDnsSuffix;
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
    public DataLakeAnalyticsSparkResourcePoolManagementClientImpl withAcceptLanguage(String acceptLanguage) {
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
    public DataLakeAnalyticsSparkResourcePoolManagementClientImpl withLongRunningOperationRetryTimeout(int longRunningOperationRetryTimeout) {
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
    public DataLakeAnalyticsSparkResourcePoolManagementClientImpl withGenerateClientRequestId(boolean generateClientRequestId) {
        this.generateClientRequestId = generateClientRequestId;
        return this;
    }

    /**
     * The SparkResourcePools object to access its operations.
     */
    private SparkResourcePools sparkResourcePools;

    /**
     * Gets the SparkResourcePools object to access its operations.
     * @return the SparkResourcePools object.
     */
    public SparkResourcePools sparkResourcePools() {
        return this.sparkResourcePools;
    }

    /**
     * Initializes an instance of DataLakeAnalyticsSparkResourcePoolManagementClient client.
     *
     * @param credentials the management credentials for Azure
     */
    public DataLakeAnalyticsSparkResourcePoolManagementClientImpl(ServiceClientCredentials credentials) {
        this("https://{accountName}.{adlaResourcePoolDnsSuffix}", credentials);
    }

    /**
     * Initializes an instance of DataLakeAnalyticsSparkResourcePoolManagementClient client.
     *
     * @param baseUrl the base URL of the host
     * @param credentials the management credentials for Azure
     */
    private DataLakeAnalyticsSparkResourcePoolManagementClientImpl(String baseUrl, ServiceClientCredentials credentials) {
        super(baseUrl, credentials);
        initialize();
    }

    /**
     * Initializes an instance of DataLakeAnalyticsSparkResourcePoolManagementClient client.
     *
     * @param restClient the REST client to connect to Azure.
     */
    public DataLakeAnalyticsSparkResourcePoolManagementClientImpl(RestClient restClient) {
        super(restClient);
        initialize();
    }

    protected void initialize() {
        this.adlaResourcePoolDnsSuffix = "azuredatalakeanalytics.net";
        this.apiVersion = "2018-02-01-preview";
        this.acceptLanguage = "en-US";
        this.longRunningOperationRetryTimeout = 30;
        this.generateClientRequestId = true;
        this.sparkResourcePools = new SparkResourcePoolsImpl(restClient().retrofit(), this);
        this.azureClient = new AzureClient(this);
    }

    /**
     * Gets the User-Agent header for the client.
     *
     * @return the user agent string.
     */
    @Override
    public String userAgent() {
        return String.format("%s (%s, %s)", super.userAgent(), "DataLakeAnalyticsSparkResourcePoolManagementClient", "2018-02-01-preview");
    }
}
