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

package com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.models.CreateSparkResourcePool;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.models.SparkResourcePool;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.models.SparkResourcePoolList;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.models.UpdateSparkResourcePoolParameters;
import com.microsoft.rest.ServiceCallback;
import com.microsoft.rest.ServiceFuture;
import com.microsoft.rest.ServiceResponse;
import java.io.IOException;
import java.util.UUID;
import rx.Observable;

/**
 * An instance of this class provides access to all the operations defined
 * in SparkResourcePools.
 */
public interface SparkResourcePools {
    /**
     * Gets all the resource pools for the account.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the SparkResourcePoolList object if successful.
     */
    SparkResourcePoolList list(String accountName);

    /**
     * Gets all the resource pools for the account.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    ServiceFuture<SparkResourcePoolList> listAsync(String accountName, final ServiceCallback<SparkResourcePoolList> serviceCallback);

    /**
     * Gets all the resource pools for the account.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePoolList object
     */
    Observable<SparkResourcePoolList> listAsync(String accountName);

    /**
     * Gets all the resource pools for the account.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePoolList object
     */
    Observable<ServiceResponse<SparkResourcePoolList>> listWithServiceResponseAsync(String accountName);

    /**
     * Submits a resource pool creation request to the specified Data Lake Analytics account.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param parameters The parameters to submit a spark resource pool creation request.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the SparkResourcePool object if successful.
     */
    SparkResourcePool create(String accountName, UUID resourcePoolId, CreateSparkResourcePool parameters);

    /**
     * Submits a resource pool creation request to the specified Data Lake Analytics account.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param parameters The parameters to submit a spark resource pool creation request.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    ServiceFuture<SparkResourcePool> createAsync(String accountName, UUID resourcePoolId, CreateSparkResourcePool parameters, final ServiceCallback<SparkResourcePool> serviceCallback);

    /**
     * Submits a resource pool creation request to the specified Data Lake Analytics account.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param parameters The parameters to submit a spark resource pool creation request.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    Observable<SparkResourcePool> createAsync(String accountName, UUID resourcePoolId, CreateSparkResourcePool parameters);

    /**
     * Submits a resource pool creation request to the specified Data Lake Analytics account.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param parameters The parameters to submit a spark resource pool creation request.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    Observable<ServiceResponse<SparkResourcePool>> createWithServiceResponseAsync(String accountName, UUID resourcePoolId, CreateSparkResourcePool parameters);

    /**
     * Gets the resource pool information for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool ID.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the SparkResourcePool object if successful.
     */
    SparkResourcePool get(String accountName, UUID resourcePoolId);

    /**
     * Gets the resource pool information for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool ID.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    ServiceFuture<SparkResourcePool> getAsync(String accountName, UUID resourcePoolId, final ServiceCallback<SparkResourcePool> serviceCallback);

    /**
     * Gets the resource pool information for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool ID.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    Observable<SparkResourcePool> getAsync(String accountName, UUID resourcePoolId);

    /**
     * Gets the resource pool information for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool ID.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    Observable<ServiceResponse<SparkResourcePool>> getWithServiceResponseAsync(String accountName, UUID resourcePoolId);

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the SparkResourcePool object if successful.
     */
    SparkResourcePool update(String accountName, UUID resourcePoolId);

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    ServiceFuture<SparkResourcePool> updateAsync(String accountName, UUID resourcePoolId, final ServiceCallback<SparkResourcePool> serviceCallback);

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    Observable<SparkResourcePool> updateAsync(String accountName, UUID resourcePoolId);

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    Observable<ServiceResponse<SparkResourcePool>> updateWithServiceResponseAsync(String accountName, UUID resourcePoolId);
    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param properties The spark resource pool specific properties.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the SparkResourcePool object if successful.
     */
    SparkResourcePool update(String accountName, UUID resourcePoolId, UpdateSparkResourcePoolParameters properties);

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param properties The spark resource pool specific properties.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    ServiceFuture<SparkResourcePool> updateAsync(String accountName, UUID resourcePoolId, UpdateSparkResourcePoolParameters properties, final ServiceCallback<SparkResourcePool> serviceCallback);

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param properties The spark resource pool specific properties.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    Observable<SparkResourcePool> updateAsync(String accountName, UUID resourcePoolId, UpdateSparkResourcePoolParameters properties);

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param properties The spark resource pool specific properties.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    Observable<ServiceResponse<SparkResourcePool>> updateWithServiceResponseAsync(String accountName, UUID resourcePoolId, UpdateSparkResourcePoolParameters properties);

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the SparkResourcePool object if successful.
     */
    SparkResourcePool beginUpdate(String accountName, UUID resourcePoolId);

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    ServiceFuture<SparkResourcePool> beginUpdateAsync(String accountName, UUID resourcePoolId, final ServiceCallback<SparkResourcePool> serviceCallback);

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    Observable<SparkResourcePool> beginUpdateAsync(String accountName, UUID resourcePoolId);

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    Observable<ServiceResponse<SparkResourcePool>> beginUpdateWithServiceResponseAsync(String accountName, UUID resourcePoolId);
    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param properties The spark resource pool specific properties.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the SparkResourcePool object if successful.
     */
    SparkResourcePool beginUpdate(String accountName, UUID resourcePoolId, UpdateSparkResourcePoolParameters properties);

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param properties The spark resource pool specific properties.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    ServiceFuture<SparkResourcePool> beginUpdateAsync(String accountName, UUID resourcePoolId, UpdateSparkResourcePoolParameters properties, final ServiceCallback<SparkResourcePool> serviceCallback);

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param properties The spark resource pool specific properties.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    Observable<SparkResourcePool> beginUpdateAsync(String accountName, UUID resourcePoolId, UpdateSparkResourcePoolParameters properties);

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param properties The spark resource pool specific properties.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    Observable<ServiceResponse<SparkResourcePool>> beginUpdateWithServiceResponseAsync(String accountName, UUID resourcePoolId, UpdateSparkResourcePoolParameters properties);

    /**
     * Cancels the running resource pool specified by the resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     */
    void stop(String accountName, UUID resourcePoolId);

    /**
     * Cancels the running resource pool specified by the resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    ServiceFuture<Void> stopAsync(String accountName, UUID resourcePoolId, final ServiceCallback<Void> serviceCallback);

    /**
     * Cancels the running resource pool specified by the resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceResponse} object if successful.
     */
    Observable<Void> stopAsync(String accountName, UUID resourcePoolId);

    /**
     * Cancels the running resource pool specified by the resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceResponse} object if successful.
     */
    Observable<ServiceResponse<Void>> stopWithServiceResponseAsync(String accountName, UUID resourcePoolId);

    /**
     * Cancels the running resource pool specified by the resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     */
    void beginStop(String accountName, UUID resourcePoolId);

    /**
     * Cancels the running resource pool specified by the resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    ServiceFuture<Void> beginStopAsync(String accountName, UUID resourcePoolId, final ServiceCallback<Void> serviceCallback);

    /**
     * Cancels the running resource pool specified by the resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceResponse} object if successful.
     */
    Observable<Void> beginStopAsync(String accountName, UUID resourcePoolId);

    /**
     * Cancels the running resource pool specified by the resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceResponse} object if successful.
     */
    Observable<ServiceResponse<Void>> beginStopWithServiceResponseAsync(String accountName, UUID resourcePoolId);

}
