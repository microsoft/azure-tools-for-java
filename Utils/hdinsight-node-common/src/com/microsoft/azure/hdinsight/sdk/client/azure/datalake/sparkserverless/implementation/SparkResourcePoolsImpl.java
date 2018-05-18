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

import retrofit2.Retrofit;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.SparkResourcePools;
import com.google.common.base.Joiner;
import com.google.common.reflect.TypeToken;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.models.CreateSparkResourcePool;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.models.SparkResourcePool;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.models.SparkResourcePoolList;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.models.UpdateSparkResourcePool;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.models.UpdateSparkResourcePoolParameters;
import com.microsoft.rest.ServiceCallback;
import com.microsoft.rest.ServiceFuture;
import com.microsoft.rest.ServiceResponse;
import com.microsoft.rest.Validator;
import java.io.IOException;
import java.util.UUID;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.HTTP;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.PUT;
import retrofit2.http.Query;
import retrofit2.Response;
import rx.functions.Func1;
import rx.Observable;

/**
 * An instance of this class provides access to all the operations defined
 * in SparkResourcePools.
 */
public class SparkResourcePoolsImpl implements SparkResourcePools {
    /** The Retrofit service to perform REST calls. */
    private SparkResourcePoolsService service;
    /** The service client containing this operation class. */
    private DataLakeAnalyticsSparkResourcePoolManagementClientImpl client;

    /**
     * Initializes an instance of SparkResourcePoolsImpl.
     *
     * @param retrofit the Retrofit instance built from a Retrofit Builder.
     * @param client the instance of the service client containing this operation class.
     */
    public SparkResourcePoolsImpl(Retrofit retrofit, DataLakeAnalyticsSparkResourcePoolManagementClientImpl client) {
        this.service = retrofit.create(SparkResourcePoolsService.class);
        this.client = client;
    }

    /**
     * The interface defining all the services for SparkResourcePools to be
     * used by Retrofit to perform actually REST calls.
     */
    interface SparkResourcePoolsService {
        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.SparkResourcePools list" })
        @GET("activityTypes/spark/resourcePools")
        Observable<Response<ResponseBody>> list(@Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Header("x-ms-parameterized-host") String parameterizedHost, @Header("User-Agent") String userAgent);

        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.SparkResourcePools create" })
        @PUT("activityTypes/spark/resourcePools/{resourcePoolId}")
        Observable<Response<ResponseBody>> create(@Path("resourcePoolId") UUID resourcePoolId, @Body CreateSparkResourcePool parameters, @Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Header("x-ms-parameterized-host") String parameterizedHost, @Header("User-Agent") String userAgent);

        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.SparkResourcePools get" })
        @GET("activityTypes/spark/resourcePools/{resourcePoolId}")
        Observable<Response<ResponseBody>> get(@Path("resourcePoolId") UUID resourcePoolId, @Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Header("x-ms-parameterized-host") String parameterizedHost, @Header("User-Agent") String userAgent);

        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.SparkResourcePools update" })
        @PATCH("activityTypes/spark/resourcePools/{resourcePoolId}")
        Observable<Response<ResponseBody>> update(@Path("resourcePoolId") UUID resourcePoolId, @Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Body UpdateSparkResourcePool parameters, @Header("x-ms-parameterized-host") String parameterizedHost, @Header("User-Agent") String userAgent);

        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.SparkResourcePools beginUpdate" })
        @PATCH("activityTypes/spark/resourcePools/{resourcePoolId}")
        Observable<Response<ResponseBody>> beginUpdate(@Path("resourcePoolId") UUID resourcePoolId, @Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Body UpdateSparkResourcePool parameters, @Header("x-ms-parameterized-host") String parameterizedHost, @Header("User-Agent") String userAgent);

        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.SparkResourcePools stop" })
        @HTTP(path = "activityTypes/spark/resourcePools/{resourcePoolId}", method = "DELETE", hasBody = true)
        Observable<Response<ResponseBody>> stop(@Path("resourcePoolId") UUID resourcePoolId, @Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Header("x-ms-parameterized-host") String parameterizedHost, @Header("User-Agent") String userAgent);

        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.hdinsight.sdk.client.azure.datalake.sparkserverless.SparkResourcePools beginStop" })
        @HTTP(path = "activityTypes/spark/resourcePools/{resourcePoolId}", method = "DELETE", hasBody = true)
        Observable<Response<ResponseBody>> beginStop(@Path("resourcePoolId") UUID resourcePoolId, @Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Header("x-ms-parameterized-host") String parameterizedHost, @Header("User-Agent") String userAgent);

    }

    /**
     * Gets all the resource pools for the account.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the SparkResourcePoolList object if successful.
     */
    public SparkResourcePoolList list(String accountName) {
        return listWithServiceResponseAsync(accountName).toBlocking().single().body();
    }

    /**
     * Gets all the resource pools for the account.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<SparkResourcePoolList> listAsync(String accountName, final ServiceCallback<SparkResourcePoolList> serviceCallback) {
        return ServiceFuture.fromResponse(listWithServiceResponseAsync(accountName), serviceCallback);
    }

    /**
     * Gets all the resource pools for the account.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePoolList object
     */
    public Observable<SparkResourcePoolList> listAsync(String accountName) {
        return listWithServiceResponseAsync(accountName).map(new Func1<ServiceResponse<SparkResourcePoolList>, SparkResourcePoolList>() {
            @Override
            public SparkResourcePoolList call(ServiceResponse<SparkResourcePoolList> response) {
                return response.body();
            }
        });
    }

    /**
     * Gets all the resource pools for the account.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePoolList object
     */
    public Observable<ServiceResponse<SparkResourcePoolList>> listWithServiceResponseAsync(String accountName) {
        if (accountName == null) {
            throw new IllegalArgumentException("Parameter accountName is required and cannot be null.");
        }
        if (this.client.adlaResourcePoolDnsSuffix() == null) {
            throw new IllegalArgumentException("Parameter this.client.adlaResourcePoolDnsSuffix() is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        String parameterizedHost = Joiner.on(", ").join("{accountName}", accountName, "{adlaResourcePoolDnsSuffix}", this.client.adlaResourcePoolDnsSuffix());
        return service.list(this.client.apiVersion(), this.client.acceptLanguage(), parameterizedHost, this.client.userAgent())
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponse<SparkResourcePoolList>>>() {
                @Override
                public Observable<ServiceResponse<SparkResourcePoolList>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponse<SparkResourcePoolList> clientResponse = listDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    private ServiceResponse<SparkResourcePoolList> listDelegate(Response<ResponseBody> response) throws CloudException, IOException, IllegalArgumentException {
        return this.client.restClient().responseBuilderFactory().<SparkResourcePoolList, CloudException>newInstance(this.client.serializerAdapter())
                .register(200, new TypeToken<SparkResourcePoolList>() { }.getType())
                .registerError(CloudException.class)
                .build(response);
    }

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
    public SparkResourcePool create(String accountName, UUID resourcePoolId, CreateSparkResourcePool parameters) {
        return createWithServiceResponseAsync(accountName, resourcePoolId, parameters).toBlocking().single().body();
    }

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
    public ServiceFuture<SparkResourcePool> createAsync(String accountName, UUID resourcePoolId, CreateSparkResourcePool parameters, final ServiceCallback<SparkResourcePool> serviceCallback) {
        return ServiceFuture.fromResponse(createWithServiceResponseAsync(accountName, resourcePoolId, parameters), serviceCallback);
    }

    /**
     * Submits a resource pool creation request to the specified Data Lake Analytics account.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param parameters The parameters to submit a spark resource pool creation request.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    public Observable<SparkResourcePool> createAsync(String accountName, UUID resourcePoolId, CreateSparkResourcePool parameters) {
        return createWithServiceResponseAsync(accountName, resourcePoolId, parameters).map(new Func1<ServiceResponse<SparkResourcePool>, SparkResourcePool>() {
            @Override
            public SparkResourcePool call(ServiceResponse<SparkResourcePool> response) {
                return response.body();
            }
        });
    }

    /**
     * Submits a resource pool creation request to the specified Data Lake Analytics account.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param parameters The parameters to submit a spark resource pool creation request.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    public Observable<ServiceResponse<SparkResourcePool>> createWithServiceResponseAsync(String accountName, UUID resourcePoolId, CreateSparkResourcePool parameters) {
        if (accountName == null) {
            throw new IllegalArgumentException("Parameter accountName is required and cannot be null.");
        }
        if (this.client.adlaResourcePoolDnsSuffix() == null) {
            throw new IllegalArgumentException("Parameter this.client.adlaResourcePoolDnsSuffix() is required and cannot be null.");
        }
        if (resourcePoolId == null) {
            throw new IllegalArgumentException("Parameter resourcePoolId is required and cannot be null.");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("Parameter parameters is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        Validator.validate(parameters);
        String parameterizedHost = Joiner.on(", ").join("{accountName}", accountName, "{adlaResourcePoolDnsSuffix}", this.client.adlaResourcePoolDnsSuffix());
        return service.create(resourcePoolId, parameters, this.client.apiVersion(), this.client.acceptLanguage(), parameterizedHost, this.client.userAgent())
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponse<SparkResourcePool>>>() {
                @Override
                public Observable<ServiceResponse<SparkResourcePool>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponse<SparkResourcePool> clientResponse = createDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    private ServiceResponse<SparkResourcePool> createDelegate(Response<ResponseBody> response) throws CloudException, IOException, IllegalArgumentException {
        return this.client.restClient().responseBuilderFactory().<SparkResourcePool, CloudException>newInstance(this.client.serializerAdapter())
                .register(200, new TypeToken<SparkResourcePool>() { }.getType())
                .registerError(CloudException.class)
                .build(response);
    }

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
    public SparkResourcePool get(String accountName, UUID resourcePoolId) {
        return getWithServiceResponseAsync(accountName, resourcePoolId).toBlocking().single().body();
    }

    /**
     * Gets the resource pool information for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool ID.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<SparkResourcePool> getAsync(String accountName, UUID resourcePoolId, final ServiceCallback<SparkResourcePool> serviceCallback) {
        return ServiceFuture.fromResponse(getWithServiceResponseAsync(accountName, resourcePoolId), serviceCallback);
    }

    /**
     * Gets the resource pool information for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool ID.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    public Observable<SparkResourcePool> getAsync(String accountName, UUID resourcePoolId) {
        return getWithServiceResponseAsync(accountName, resourcePoolId).map(new Func1<ServiceResponse<SparkResourcePool>, SparkResourcePool>() {
            @Override
            public SparkResourcePool call(ServiceResponse<SparkResourcePool> response) {
                return response.body();
            }
        });
    }

    /**
     * Gets the resource pool information for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool ID.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    public Observable<ServiceResponse<SparkResourcePool>> getWithServiceResponseAsync(String accountName, UUID resourcePoolId) {
        if (accountName == null) {
            throw new IllegalArgumentException("Parameter accountName is required and cannot be null.");
        }
        if (this.client.adlaResourcePoolDnsSuffix() == null) {
            throw new IllegalArgumentException("Parameter this.client.adlaResourcePoolDnsSuffix() is required and cannot be null.");
        }
        if (resourcePoolId == null) {
            throw new IllegalArgumentException("Parameter resourcePoolId is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        String parameterizedHost = Joiner.on(", ").join("{accountName}", accountName, "{adlaResourcePoolDnsSuffix}", this.client.adlaResourcePoolDnsSuffix());
        return service.get(resourcePoolId, this.client.apiVersion(), this.client.acceptLanguage(), parameterizedHost, this.client.userAgent())
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponse<SparkResourcePool>>>() {
                @Override
                public Observable<ServiceResponse<SparkResourcePool>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponse<SparkResourcePool> clientResponse = getDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    private ServiceResponse<SparkResourcePool> getDelegate(Response<ResponseBody> response) throws CloudException, IOException, IllegalArgumentException {
        return this.client.restClient().responseBuilderFactory().<SparkResourcePool, CloudException>newInstance(this.client.serializerAdapter())
                .register(200, new TypeToken<SparkResourcePool>() { }.getType())
                .registerError(CloudException.class)
                .build(response);
    }

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
    public SparkResourcePool update(String accountName, UUID resourcePoolId) {
        return updateWithServiceResponseAsync(accountName, resourcePoolId).toBlocking().last().body();
    }

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<SparkResourcePool> updateAsync(String accountName, UUID resourcePoolId, final ServiceCallback<SparkResourcePool> serviceCallback) {
        return ServiceFuture.fromResponse(updateWithServiceResponseAsync(accountName, resourcePoolId), serviceCallback);
    }

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable for the request
     */
    public Observable<SparkResourcePool> updateAsync(String accountName, UUID resourcePoolId) {
        return updateWithServiceResponseAsync(accountName, resourcePoolId).map(new Func1<ServiceResponse<SparkResourcePool>, SparkResourcePool>() {
            @Override
            public SparkResourcePool call(ServiceResponse<SparkResourcePool> response) {
                return response.body();
            }
        });
    }

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable for the request
     */
    public Observable<ServiceResponse<SparkResourcePool>> updateWithServiceResponseAsync(String accountName, UUID resourcePoolId) {
        if (accountName == null) {
            throw new IllegalArgumentException("Parameter accountName is required and cannot be null.");
        }
        if (this.client.adlaResourcePoolDnsSuffix() == null) {
            throw new IllegalArgumentException("Parameter this.client.adlaResourcePoolDnsSuffix() is required and cannot be null.");
        }
        if (resourcePoolId == null) {
            throw new IllegalArgumentException("Parameter resourcePoolId is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        final UpdateSparkResourcePoolParameters properties = null;
        UpdateSparkResourcePool parameters = new UpdateSparkResourcePool();
        parameters.withProperties(null);
        Observable<Response<ResponseBody>> observable = service.update(resourcePoolId, this.client.apiVersion(), this.client.acceptLanguage(), parameters, parameterizedHost, this.client.userAgent());
        return client.getAzureClient().getPutOrPatchResultAsync(observable, new TypeToken<SparkResourcePool>() { }.getType());
    }
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
    public SparkResourcePool update(String accountName, UUID resourcePoolId, UpdateSparkResourcePoolParameters properties) {
        return updateWithServiceResponseAsync(accountName, resourcePoolId, properties).toBlocking().last().body();
    }

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
    public ServiceFuture<SparkResourcePool> updateAsync(String accountName, UUID resourcePoolId, UpdateSparkResourcePoolParameters properties, final ServiceCallback<SparkResourcePool> serviceCallback) {
        return ServiceFuture.fromResponse(updateWithServiceResponseAsync(accountName, resourcePoolId, properties), serviceCallback);
    }

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param properties The spark resource pool specific properties.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable for the request
     */
    public Observable<SparkResourcePool> updateAsync(String accountName, UUID resourcePoolId, UpdateSparkResourcePoolParameters properties) {
        return updateWithServiceResponseAsync(accountName, resourcePoolId, properties).map(new Func1<ServiceResponse<SparkResourcePool>, SparkResourcePool>() {
            @Override
            public SparkResourcePool call(ServiceResponse<SparkResourcePool> response) {
                return response.body();
            }
        });
    }

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param properties The spark resource pool specific properties.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable for the request
     */
    public Observable<ServiceResponse<SparkResourcePool>> updateWithServiceResponseAsync(String accountName, UUID resourcePoolId, UpdateSparkResourcePoolParameters properties) {
        if (accountName == null) {
            throw new IllegalArgumentException("Parameter accountName is required and cannot be null.");
        }
        if (this.client.adlaResourcePoolDnsSuffix() == null) {
            throw new IllegalArgumentException("Parameter this.client.adlaResourcePoolDnsSuffix() is required and cannot be null.");
        }
        if (resourcePoolId == null) {
            throw new IllegalArgumentException("Parameter resourcePoolId is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        Validator.validate(properties);
        UpdateSparkResourcePool parameters = null;
        if (properties != null) {
            parameters = new UpdateSparkResourcePool();
            parameters.withProperties(properties);
        }
        Observable<Response<ResponseBody>> observable = service.update(resourcePoolId, this.client.apiVersion(), this.client.acceptLanguage(), parameters, parameterizedHost, this.client.userAgent());
        return client.getAzureClient().getPutOrPatchResultAsync(observable, new TypeToken<SparkResourcePool>() { }.getType());
    }

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
    public SparkResourcePool beginUpdate(String accountName, UUID resourcePoolId) {
        return beginUpdateWithServiceResponseAsync(accountName, resourcePoolId).toBlocking().single().body();
    }

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<SparkResourcePool> beginUpdateAsync(String accountName, UUID resourcePoolId, final ServiceCallback<SparkResourcePool> serviceCallback) {
        return ServiceFuture.fromResponse(beginUpdateWithServiceResponseAsync(accountName, resourcePoolId), serviceCallback);
    }

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    public Observable<SparkResourcePool> beginUpdateAsync(String accountName, UUID resourcePoolId) {
        return beginUpdateWithServiceResponseAsync(accountName, resourcePoolId).map(new Func1<ServiceResponse<SparkResourcePool>, SparkResourcePool>() {
            @Override
            public SparkResourcePool call(ServiceResponse<SparkResourcePool> response) {
                return response.body();
            }
        });
    }

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    public Observable<ServiceResponse<SparkResourcePool>> beginUpdateWithServiceResponseAsync(String accountName, UUID resourcePoolId) {
        if (accountName == null) {
            throw new IllegalArgumentException("Parameter accountName is required and cannot be null.");
        }
        if (this.client.adlaResourcePoolDnsSuffix() == null) {
            throw new IllegalArgumentException("Parameter this.client.adlaResourcePoolDnsSuffix() is required and cannot be null.");
        }
        if (resourcePoolId == null) {
            throw new IllegalArgumentException("Parameter resourcePoolId is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        final UpdateSparkResourcePoolParameters properties = null;
        UpdateSparkResourcePool parameters = new UpdateSparkResourcePool();
        parameters.withProperties(null);
        String parameterizedHost = Joiner.on(", ").join("{accountName}", accountName, "{adlaResourcePoolDnsSuffix}", this.client.adlaResourcePoolDnsSuffix());
        return service.beginUpdate(resourcePoolId, this.client.apiVersion(), this.client.acceptLanguage(), parameters, parameterizedHost, this.client.userAgent())
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponse<SparkResourcePool>>>() {
                @Override
                public Observable<ServiceResponse<SparkResourcePool>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponse<SparkResourcePool> clientResponse = beginUpdateDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

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
    public SparkResourcePool beginUpdate(String accountName, UUID resourcePoolId, UpdateSparkResourcePoolParameters properties) {
        return beginUpdateWithServiceResponseAsync(accountName, resourcePoolId, properties).toBlocking().single().body();
    }

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
    public ServiceFuture<SparkResourcePool> beginUpdateAsync(String accountName, UUID resourcePoolId, UpdateSparkResourcePoolParameters properties, final ServiceCallback<SparkResourcePool> serviceCallback) {
        return ServiceFuture.fromResponse(beginUpdateWithServiceResponseAsync(accountName, resourcePoolId, properties), serviceCallback);
    }

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param properties The spark resource pool specific properties.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    public Observable<SparkResourcePool> beginUpdateAsync(String accountName, UUID resourcePoolId, UpdateSparkResourcePoolParameters properties) {
        return beginUpdateWithServiceResponseAsync(accountName, resourcePoolId, properties).map(new Func1<ServiceResponse<SparkResourcePool>, SparkResourcePool>() {
            @Override
            public SparkResourcePool call(ServiceResponse<SparkResourcePool> response) {
                return response.body();
            }
        });
    }

    /**
     * Updates the resource pool for the specified resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param properties The spark resource pool specific properties.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the SparkResourcePool object
     */
    public Observable<ServiceResponse<SparkResourcePool>> beginUpdateWithServiceResponseAsync(String accountName, UUID resourcePoolId, UpdateSparkResourcePoolParameters properties) {
        if (accountName == null) {
            throw new IllegalArgumentException("Parameter accountName is required and cannot be null.");
        }
        if (this.client.adlaResourcePoolDnsSuffix() == null) {
            throw new IllegalArgumentException("Parameter this.client.adlaResourcePoolDnsSuffix() is required and cannot be null.");
        }
        if (resourcePoolId == null) {
            throw new IllegalArgumentException("Parameter resourcePoolId is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        Validator.validate(properties);
        UpdateSparkResourcePool parameters = null;
        if (properties != null) {
            parameters = new UpdateSparkResourcePool();
            parameters.withProperties(properties);
        }
        String parameterizedHost = Joiner.on(", ").join("{accountName}", accountName, "{adlaResourcePoolDnsSuffix}", this.client.adlaResourcePoolDnsSuffix());
        return service.beginUpdate(resourcePoolId, this.client.apiVersion(), this.client.acceptLanguage(), parameters, parameterizedHost, this.client.userAgent())
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponse<SparkResourcePool>>>() {
                @Override
                public Observable<ServiceResponse<SparkResourcePool>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponse<SparkResourcePool> clientResponse = beginUpdateDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    private ServiceResponse<SparkResourcePool> beginUpdateDelegate(Response<ResponseBody> response) throws CloudException, IOException, IllegalArgumentException {
        return this.client.restClient().responseBuilderFactory().<SparkResourcePool, CloudException>newInstance(this.client.serializerAdapter())
                .register(200, new TypeToken<SparkResourcePool>() { }.getType())
                .register(201, new TypeToken<Void>() { }.getType())
                .register(202, new TypeToken<Void>() { }.getType())
                .registerError(CloudException.class)
                .build(response);
    }

    /**
     * Cancels the running resource pool specified by the resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     */
    public void stop(String accountName, UUID resourcePoolId) {
        stopWithServiceResponseAsync(accountName, resourcePoolId).toBlocking().last().body();
    }

    /**
     * Cancels the running resource pool specified by the resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<Void> stopAsync(String accountName, UUID resourcePoolId, final ServiceCallback<Void> serviceCallback) {
        return ServiceFuture.fromResponse(stopWithServiceResponseAsync(accountName, resourcePoolId), serviceCallback);
    }

    /**
     * Cancels the running resource pool specified by the resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable for the request
     */
    public Observable<Void> stopAsync(String accountName, UUID resourcePoolId) {
        return stopWithServiceResponseAsync(accountName, resourcePoolId).map(new Func1<ServiceResponse<Void>, Void>() {
            @Override
            public Void call(ServiceResponse<Void> response) {
                return response.body();
            }
        });
    }

    /**
     * Cancels the running resource pool specified by the resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable for the request
     */
    public Observable<ServiceResponse<Void>> stopWithServiceResponseAsync(String accountName, UUID resourcePoolId) {
        if (accountName == null) {
            throw new IllegalArgumentException("Parameter accountName is required and cannot be null.");
        }
        if (this.client.adlaResourcePoolDnsSuffix() == null) {
            throw new IllegalArgumentException("Parameter this.client.adlaResourcePoolDnsSuffix() is required and cannot be null.");
        }
        if (resourcePoolId == null) {
            throw new IllegalArgumentException("Parameter resourcePoolId is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        Observable<Response<ResponseBody>> observable = service.stop(resourcePoolId, this.client.apiVersion(), this.client.acceptLanguage(), parameterizedHost, this.client.userAgent());
        return client.getAzureClient().getPostOrDeleteResultAsync(observable, new TypeToken<Void>() { }.getType());
    }

    /**
     * Cancels the running resource pool specified by the resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     */
    public void beginStop(String accountName, UUID resourcePoolId) {
        beginStopWithServiceResponseAsync(accountName, resourcePoolId).toBlocking().single().body();
    }

    /**
     * Cancels the running resource pool specified by the resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<Void> beginStopAsync(String accountName, UUID resourcePoolId, final ServiceCallback<Void> serviceCallback) {
        return ServiceFuture.fromResponse(beginStopWithServiceResponseAsync(accountName, resourcePoolId), serviceCallback);
    }

    /**
     * Cancels the running resource pool specified by the resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceResponse} object if successful.
     */
    public Observable<Void> beginStopAsync(String accountName, UUID resourcePoolId) {
        return beginStopWithServiceResponseAsync(accountName, resourcePoolId).map(new Func1<ServiceResponse<Void>, Void>() {
            @Override
            public Void call(ServiceResponse<Void> response) {
                return response.body();
            }
        });
    }

    /**
     * Cancels the running resource pool specified by the resource pool ID.
     *
     * @param accountName The Azure Data Lake Analytics account to execute spark resource pool operations on.
     * @param resourcePoolId resource pool identifier. Uniquely identifies the resource pool across all resource pools submitted to the service.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceResponse} object if successful.
     */
    public Observable<ServiceResponse<Void>> beginStopWithServiceResponseAsync(String accountName, UUID resourcePoolId) {
        if (accountName == null) {
            throw new IllegalArgumentException("Parameter accountName is required and cannot be null.");
        }
        if (this.client.adlaResourcePoolDnsSuffix() == null) {
            throw new IllegalArgumentException("Parameter this.client.adlaResourcePoolDnsSuffix() is required and cannot be null.");
        }
        if (resourcePoolId == null) {
            throw new IllegalArgumentException("Parameter resourcePoolId is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        String parameterizedHost = Joiner.on(", ").join("{accountName}", accountName, "{adlaResourcePoolDnsSuffix}", this.client.adlaResourcePoolDnsSuffix());
        return service.beginStop(resourcePoolId, this.client.apiVersion(), this.client.acceptLanguage(), parameterizedHost, this.client.userAgent())
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponse<Void>>>() {
                @Override
                public Observable<ServiceResponse<Void>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponse<Void> clientResponse = beginStopDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    private ServiceResponse<Void> beginStopDelegate(Response<ResponseBody> response) throws CloudException, IOException, IllegalArgumentException {
        return this.client.restClient().responseBuilderFactory().<Void, CloudException>newInstance(this.client.serializerAdapter())
                .register(200, new TypeToken<Void>() { }.getType())
                .register(202, new TypeToken<Void>() { }.getType())
                .register(204, new TypeToken<Void>() { }.getType())
                .registerError(CloudException.class)
                .build(response);
    }

}
