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

import retrofit2.Retrofit;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.ComputePolicies;
import com.google.common.reflect.TypeToken;
import com.microsoft.azure.AzureServiceFuture;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.models.ComputePolicy;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.models.CreateOrUpdateComputePolicyParameters;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.models.PageImpl;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.models.UpdateComputePolicyParameters;
import com.microsoft.azure.ListOperationCallback;
import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.rest.ServiceCallback;
import com.microsoft.rest.ServiceFuture;
import com.microsoft.rest.ServiceResponse;
import com.microsoft.rest.Validator;
import java.io.IOException;
import java.util.List;
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
import retrofit2.http.Url;
import retrofit2.Response;
import rx.functions.Func1;
import rx.Observable;

/**
 * An instance of this class provides access to all the operations defined
 * in ComputePolicies.
 */
public class ComputePoliciesImpl implements ComputePolicies {
    /** The Retrofit service to perform REST calls. */
    private ComputePoliciesService service;
    /** The service client containing this operation class. */
    private DataLakeAnalyticsAccountManagementClientImpl client;

    /**
     * Initializes an instance of ComputePoliciesImpl.
     *
     * @param retrofit the Retrofit instance built from a Retrofit Builder.
     * @param client the instance of the service client containing this operation class.
     */
    public ComputePoliciesImpl(Retrofit retrofit, DataLakeAnalyticsAccountManagementClientImpl client) {
        this.service = retrofit.create(ComputePoliciesService.class);
        this.client = client;
    }

    /**
     * The interface defining all the services for ComputePolicies to be
     * used by Retrofit to perform actually REST calls.
     */
    interface ComputePoliciesService {
        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.ComputePolicies listByAccount" })
        @GET("subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/providers/Microsoft.DataLakeAnalytics/accounts/{accountName}/computePolicies")
        Observable<Response<ResponseBody>> listByAccount(@Path("subscriptionId") String subscriptionId, @Path("resourceGroupName") String resourceGroupName, @Path("accountName") String accountName, @Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Header("User-Agent") String userAgent);

        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.ComputePolicies createOrUpdate" })
        @PUT("subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/providers/Microsoft.DataLakeAnalytics/accounts/{accountName}/computePolicies/{computePolicyName}")
        Observable<Response<ResponseBody>> createOrUpdate(@Path("subscriptionId") String subscriptionId, @Path("resourceGroupName") String resourceGroupName, @Path("accountName") String accountName, @Path("computePolicyName") String computePolicyName, @Body CreateOrUpdateComputePolicyParameters parameters, @Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Header("User-Agent") String userAgent);

        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.ComputePolicies get" })
        @GET("subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/providers/Microsoft.DataLakeAnalytics/accounts/{accountName}/computePolicies/{computePolicyName}")
        Observable<Response<ResponseBody>> get(@Path("subscriptionId") String subscriptionId, @Path("resourceGroupName") String resourceGroupName, @Path("accountName") String accountName, @Path("computePolicyName") String computePolicyName, @Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Header("User-Agent") String userAgent);

        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.ComputePolicies update" })
        @PATCH("subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/providers/Microsoft.DataLakeAnalytics/accounts/{accountName}/computePolicies/{computePolicyName}")
        Observable<Response<ResponseBody>> update(@Path("subscriptionId") String subscriptionId, @Path("resourceGroupName") String resourceGroupName, @Path("accountName") String accountName, @Path("computePolicyName") String computePolicyName, @Body UpdateComputePolicyParameters parameters, @Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Header("User-Agent") String userAgent);

        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.ComputePolicies delete" })
        @HTTP(path = "subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/providers/Microsoft.DataLakeAnalytics/accounts/{accountName}/computePolicies/{computePolicyName}", method = "DELETE", hasBody = true)
        Observable<Response<ResponseBody>> delete(@Path("subscriptionId") String subscriptionId, @Path("resourceGroupName") String resourceGroupName, @Path("accountName") String accountName, @Path("computePolicyName") String computePolicyName, @Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Header("User-Agent") String userAgent);

        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.ComputePolicies listByAccountNext" })
        @GET
        Observable<Response<ResponseBody>> listByAccountNext(@Url String nextUrl, @Header("accept-language") String acceptLanguage, @Header("User-Agent") String userAgent);

    }

    /**
     * Lists the Data Lake Analytics compute policies within the specified Data Lake Analytics account. An account supports, at most, 50 policies.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the PagedList&lt;ComputePolicy&gt; object if successful.
     */
    public PagedList<ComputePolicy> listByAccount(final String resourceGroupName, final String accountName) {
        ServiceResponse<Page<ComputePolicy>> response = listByAccountSinglePageAsync(resourceGroupName, accountName).toBlocking().single();
        return new PagedList<ComputePolicy>(response.body()) {
            @Override
            public Page<ComputePolicy> nextPage(String nextPageLink) {
                return listByAccountNextSinglePageAsync(nextPageLink).toBlocking().single().body();
            }
        };
    }

    /**
     * Lists the Data Lake Analytics compute policies within the specified Data Lake Analytics account. An account supports, at most, 50 policies.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<List<ComputePolicy>> listByAccountAsync(final String resourceGroupName, final String accountName, final ListOperationCallback<ComputePolicy> serviceCallback) {
        return AzureServiceFuture.fromPageResponse(
            listByAccountSinglePageAsync(resourceGroupName, accountName),
            new Func1<String, Observable<ServiceResponse<Page<ComputePolicy>>>>() {
                @Override
                public Observable<ServiceResponse<Page<ComputePolicy>>> call(String nextPageLink) {
                    return listByAccountNextSinglePageAsync(nextPageLink);
                }
            },
            serviceCallback);
    }

    /**
     * Lists the Data Lake Analytics compute policies within the specified Data Lake Analytics account. An account supports, at most, 50 policies.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the PagedList&lt;ComputePolicy&gt; object
     */
    public Observable<Page<ComputePolicy>> listByAccountAsync(final String resourceGroupName, final String accountName) {
        return listByAccountWithServiceResponseAsync(resourceGroupName, accountName)
            .map(new Func1<ServiceResponse<Page<ComputePolicy>>, Page<ComputePolicy>>() {
                @Override
                public Page<ComputePolicy> call(ServiceResponse<Page<ComputePolicy>> response) {
                    return response.body();
                }
            });
    }

    /**
     * Lists the Data Lake Analytics compute policies within the specified Data Lake Analytics account. An account supports, at most, 50 policies.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the PagedList&lt;ComputePolicy&gt; object
     */
    public Observable<ServiceResponse<Page<ComputePolicy>>> listByAccountWithServiceResponseAsync(final String resourceGroupName, final String accountName) {
        return listByAccountSinglePageAsync(resourceGroupName, accountName)
            .concatMap(new Func1<ServiceResponse<Page<ComputePolicy>>, Observable<ServiceResponse<Page<ComputePolicy>>>>() {
                @Override
                public Observable<ServiceResponse<Page<ComputePolicy>>> call(ServiceResponse<Page<ComputePolicy>> page) {
                    String nextPageLink = page.body().nextPageLink();
                    if (nextPageLink == null) {
                        return Observable.just(page);
                    }
                    return Observable.just(page).concatWith(listByAccountNextWithServiceResponseAsync(nextPageLink));
                }
            });
    }

    /**
     * Lists the Data Lake Analytics compute policies within the specified Data Lake Analytics account. An account supports, at most, 50 policies.
     *
    ServiceResponse<PageImpl<ComputePolicy>> * @param resourceGroupName The name of the Azure resource group.
    ServiceResponse<PageImpl<ComputePolicy>> * @param accountName The name of the Data Lake Analytics account.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the PagedList&lt;ComputePolicy&gt; object wrapped in {@link ServiceResponse} if successful.
     */
    public Observable<ServiceResponse<Page<ComputePolicy>>> listByAccountSinglePageAsync(final String resourceGroupName, final String accountName) {
        if (this.client.subscriptionId() == null) {
            throw new IllegalArgumentException("Parameter this.client.subscriptionId() is required and cannot be null.");
        }
        if (resourceGroupName == null) {
            throw new IllegalArgumentException("Parameter resourceGroupName is required and cannot be null.");
        }
        if (accountName == null) {
            throw new IllegalArgumentException("Parameter accountName is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        return service.listByAccount(this.client.subscriptionId(), resourceGroupName, accountName, this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent())
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponse<Page<ComputePolicy>>>>() {
                @Override
                public Observable<ServiceResponse<Page<ComputePolicy>>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponse<PageImpl<ComputePolicy>> result = listByAccountDelegate(response);
                        return Observable.just(new ServiceResponse<Page<ComputePolicy>>(result.body(), result.response()));
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    private ServiceResponse<PageImpl<ComputePolicy>> listByAccountDelegate(Response<ResponseBody> response) throws CloudException, IOException, IllegalArgumentException {
        return this.client.restClient().responseBuilderFactory().<PageImpl<ComputePolicy>, CloudException>newInstance(this.client.serializerAdapter())
                .register(200, new TypeToken<PageImpl<ComputePolicy>>() { }.getType())
                .registerError(CloudException.class)
                .build(response);
    }

    /**
     * Creates or updates the specified compute policy. During update, the compute policy with the specified name will be replaced with this new compute policy. An account supports, at most, 50 policies.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to create or update.
     * @param parameters Parameters supplied to create or update the compute policy. The max degree of parallelism per job property, min priority per job property, or both must be present.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the ComputePolicy object if successful.
     */
    public ComputePolicy createOrUpdate(String resourceGroupName, String accountName, String computePolicyName, CreateOrUpdateComputePolicyParameters parameters) {
        return createOrUpdateWithServiceResponseAsync(resourceGroupName, accountName, computePolicyName, parameters).toBlocking().single().body();
    }

    /**
     * Creates or updates the specified compute policy. During update, the compute policy with the specified name will be replaced with this new compute policy. An account supports, at most, 50 policies.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to create or update.
     * @param parameters Parameters supplied to create or update the compute policy. The max degree of parallelism per job property, min priority per job property, or both must be present.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<ComputePolicy> createOrUpdateAsync(String resourceGroupName, String accountName, String computePolicyName, CreateOrUpdateComputePolicyParameters parameters, final ServiceCallback<ComputePolicy> serviceCallback) {
        return ServiceFuture.fromResponse(createOrUpdateWithServiceResponseAsync(resourceGroupName, accountName, computePolicyName, parameters), serviceCallback);
    }

    /**
     * Creates or updates the specified compute policy. During update, the compute policy with the specified name will be replaced with this new compute policy. An account supports, at most, 50 policies.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to create or update.
     * @param parameters Parameters supplied to create or update the compute policy. The max degree of parallelism per job property, min priority per job property, or both must be present.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the ComputePolicy object
     */
    public Observable<ComputePolicy> createOrUpdateAsync(String resourceGroupName, String accountName, String computePolicyName, CreateOrUpdateComputePolicyParameters parameters) {
        return createOrUpdateWithServiceResponseAsync(resourceGroupName, accountName, computePolicyName, parameters).map(new Func1<ServiceResponse<ComputePolicy>, ComputePolicy>() {
            @Override
            public ComputePolicy call(ServiceResponse<ComputePolicy> response) {
                return response.body();
            }
        });
    }

    /**
     * Creates or updates the specified compute policy. During update, the compute policy with the specified name will be replaced with this new compute policy. An account supports, at most, 50 policies.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to create or update.
     * @param parameters Parameters supplied to create or update the compute policy. The max degree of parallelism per job property, min priority per job property, or both must be present.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the ComputePolicy object
     */
    public Observable<ServiceResponse<ComputePolicy>> createOrUpdateWithServiceResponseAsync(String resourceGroupName, String accountName, String computePolicyName, CreateOrUpdateComputePolicyParameters parameters) {
        if (this.client.subscriptionId() == null) {
            throw new IllegalArgumentException("Parameter this.client.subscriptionId() is required and cannot be null.");
        }
        if (resourceGroupName == null) {
            throw new IllegalArgumentException("Parameter resourceGroupName is required and cannot be null.");
        }
        if (accountName == null) {
            throw new IllegalArgumentException("Parameter accountName is required and cannot be null.");
        }
        if (computePolicyName == null) {
            throw new IllegalArgumentException("Parameter computePolicyName is required and cannot be null.");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("Parameter parameters is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        Validator.validate(parameters);
        return service.createOrUpdate(this.client.subscriptionId(), resourceGroupName, accountName, computePolicyName, parameters, this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent())
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponse<ComputePolicy>>>() {
                @Override
                public Observable<ServiceResponse<ComputePolicy>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponse<ComputePolicy> clientResponse = createOrUpdateDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    private ServiceResponse<ComputePolicy> createOrUpdateDelegate(Response<ResponseBody> response) throws CloudException, IOException, IllegalArgumentException {
        return this.client.restClient().responseBuilderFactory().<ComputePolicy, CloudException>newInstance(this.client.serializerAdapter())
                .register(200, new TypeToken<ComputePolicy>() { }.getType())
                .registerError(CloudException.class)
                .build(response);
    }

    /**
     * Gets the specified Data Lake Analytics compute policy.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to retrieve.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the ComputePolicy object if successful.
     */
    public ComputePolicy get(String resourceGroupName, String accountName, String computePolicyName) {
        return getWithServiceResponseAsync(resourceGroupName, accountName, computePolicyName).toBlocking().single().body();
    }

    /**
     * Gets the specified Data Lake Analytics compute policy.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to retrieve.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<ComputePolicy> getAsync(String resourceGroupName, String accountName, String computePolicyName, final ServiceCallback<ComputePolicy> serviceCallback) {
        return ServiceFuture.fromResponse(getWithServiceResponseAsync(resourceGroupName, accountName, computePolicyName), serviceCallback);
    }

    /**
     * Gets the specified Data Lake Analytics compute policy.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to retrieve.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the ComputePolicy object
     */
    public Observable<ComputePolicy> getAsync(String resourceGroupName, String accountName, String computePolicyName) {
        return getWithServiceResponseAsync(resourceGroupName, accountName, computePolicyName).map(new Func1<ServiceResponse<ComputePolicy>, ComputePolicy>() {
            @Override
            public ComputePolicy call(ServiceResponse<ComputePolicy> response) {
                return response.body();
            }
        });
    }

    /**
     * Gets the specified Data Lake Analytics compute policy.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to retrieve.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the ComputePolicy object
     */
    public Observable<ServiceResponse<ComputePolicy>> getWithServiceResponseAsync(String resourceGroupName, String accountName, String computePolicyName) {
        if (this.client.subscriptionId() == null) {
            throw new IllegalArgumentException("Parameter this.client.subscriptionId() is required and cannot be null.");
        }
        if (resourceGroupName == null) {
            throw new IllegalArgumentException("Parameter resourceGroupName is required and cannot be null.");
        }
        if (accountName == null) {
            throw new IllegalArgumentException("Parameter accountName is required and cannot be null.");
        }
        if (computePolicyName == null) {
            throw new IllegalArgumentException("Parameter computePolicyName is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        return service.get(this.client.subscriptionId(), resourceGroupName, accountName, computePolicyName, this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent())
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponse<ComputePolicy>>>() {
                @Override
                public Observable<ServiceResponse<ComputePolicy>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponse<ComputePolicy> clientResponse = getDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    private ServiceResponse<ComputePolicy> getDelegate(Response<ResponseBody> response) throws CloudException, IOException, IllegalArgumentException {
        return this.client.restClient().responseBuilderFactory().<ComputePolicy, CloudException>newInstance(this.client.serializerAdapter())
                .register(200, new TypeToken<ComputePolicy>() { }.getType())
                .registerError(CloudException.class)
                .build(response);
    }

    /**
     * Updates the specified compute policy.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to update.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the ComputePolicy object if successful.
     */
    public ComputePolicy update(String resourceGroupName, String accountName, String computePolicyName) {
        return updateWithServiceResponseAsync(resourceGroupName, accountName, computePolicyName).toBlocking().single().body();
    }

    /**
     * Updates the specified compute policy.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to update.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<ComputePolicy> updateAsync(String resourceGroupName, String accountName, String computePolicyName, final ServiceCallback<ComputePolicy> serviceCallback) {
        return ServiceFuture.fromResponse(updateWithServiceResponseAsync(resourceGroupName, accountName, computePolicyName), serviceCallback);
    }

    /**
     * Updates the specified compute policy.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to update.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the ComputePolicy object
     */
    public Observable<ComputePolicy> updateAsync(String resourceGroupName, String accountName, String computePolicyName) {
        return updateWithServiceResponseAsync(resourceGroupName, accountName, computePolicyName).map(new Func1<ServiceResponse<ComputePolicy>, ComputePolicy>() {
            @Override
            public ComputePolicy call(ServiceResponse<ComputePolicy> response) {
                return response.body();
            }
        });
    }

    /**
     * Updates the specified compute policy.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to update.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the ComputePolicy object
     */
    public Observable<ServiceResponse<ComputePolicy>> updateWithServiceResponseAsync(String resourceGroupName, String accountName, String computePolicyName) {
        if (this.client.subscriptionId() == null) {
            throw new IllegalArgumentException("Parameter this.client.subscriptionId() is required and cannot be null.");
        }
        if (resourceGroupName == null) {
            throw new IllegalArgumentException("Parameter resourceGroupName is required and cannot be null.");
        }
        if (accountName == null) {
            throw new IllegalArgumentException("Parameter accountName is required and cannot be null.");
        }
        if (computePolicyName == null) {
            throw new IllegalArgumentException("Parameter computePolicyName is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        final UpdateComputePolicyParameters parameters = null;
        return service.update(this.client.subscriptionId(), resourceGroupName, accountName, computePolicyName, parameters, this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent())
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponse<ComputePolicy>>>() {
                @Override
                public Observable<ServiceResponse<ComputePolicy>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponse<ComputePolicy> clientResponse = updateDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    /**
     * Updates the specified compute policy.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to update.
     * @param parameters Parameters supplied to update the compute policy.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the ComputePolicy object if successful.
     */
    public ComputePolicy update(String resourceGroupName, String accountName, String computePolicyName, UpdateComputePolicyParameters parameters) {
        return updateWithServiceResponseAsync(resourceGroupName, accountName, computePolicyName, parameters).toBlocking().single().body();
    }

    /**
     * Updates the specified compute policy.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to update.
     * @param parameters Parameters supplied to update the compute policy.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<ComputePolicy> updateAsync(String resourceGroupName, String accountName, String computePolicyName, UpdateComputePolicyParameters parameters, final ServiceCallback<ComputePolicy> serviceCallback) {
        return ServiceFuture.fromResponse(updateWithServiceResponseAsync(resourceGroupName, accountName, computePolicyName, parameters), serviceCallback);
    }

    /**
     * Updates the specified compute policy.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to update.
     * @param parameters Parameters supplied to update the compute policy.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the ComputePolicy object
     */
    public Observable<ComputePolicy> updateAsync(String resourceGroupName, String accountName, String computePolicyName, UpdateComputePolicyParameters parameters) {
        return updateWithServiceResponseAsync(resourceGroupName, accountName, computePolicyName, parameters).map(new Func1<ServiceResponse<ComputePolicy>, ComputePolicy>() {
            @Override
            public ComputePolicy call(ServiceResponse<ComputePolicy> response) {
                return response.body();
            }
        });
    }

    /**
     * Updates the specified compute policy.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to update.
     * @param parameters Parameters supplied to update the compute policy.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the ComputePolicy object
     */
    public Observable<ServiceResponse<ComputePolicy>> updateWithServiceResponseAsync(String resourceGroupName, String accountName, String computePolicyName, UpdateComputePolicyParameters parameters) {
        if (this.client.subscriptionId() == null) {
            throw new IllegalArgumentException("Parameter this.client.subscriptionId() is required and cannot be null.");
        }
        if (resourceGroupName == null) {
            throw new IllegalArgumentException("Parameter resourceGroupName is required and cannot be null.");
        }
        if (accountName == null) {
            throw new IllegalArgumentException("Parameter accountName is required and cannot be null.");
        }
        if (computePolicyName == null) {
            throw new IllegalArgumentException("Parameter computePolicyName is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        Validator.validate(parameters);
        return service.update(this.client.subscriptionId(), resourceGroupName, accountName, computePolicyName, parameters, this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent())
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponse<ComputePolicy>>>() {
                @Override
                public Observable<ServiceResponse<ComputePolicy>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponse<ComputePolicy> clientResponse = updateDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    private ServiceResponse<ComputePolicy> updateDelegate(Response<ResponseBody> response) throws CloudException, IOException, IllegalArgumentException {
        return this.client.restClient().responseBuilderFactory().<ComputePolicy, CloudException>newInstance(this.client.serializerAdapter())
                .register(200, new TypeToken<ComputePolicy>() { }.getType())
                .registerError(CloudException.class)
                .build(response);
    }

    /**
     * Deletes the specified compute policy from the specified Data Lake Analytics account.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to delete.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     */
    public void delete(String resourceGroupName, String accountName, String computePolicyName) {
        deleteWithServiceResponseAsync(resourceGroupName, accountName, computePolicyName).toBlocking().single().body();
    }

    /**
     * Deletes the specified compute policy from the specified Data Lake Analytics account.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to delete.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<Void> deleteAsync(String resourceGroupName, String accountName, String computePolicyName, final ServiceCallback<Void> serviceCallback) {
        return ServiceFuture.fromResponse(deleteWithServiceResponseAsync(resourceGroupName, accountName, computePolicyName), serviceCallback);
    }

    /**
     * Deletes the specified compute policy from the specified Data Lake Analytics account.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to delete.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceResponse} object if successful.
     */
    public Observable<Void> deleteAsync(String resourceGroupName, String accountName, String computePolicyName) {
        return deleteWithServiceResponseAsync(resourceGroupName, accountName, computePolicyName).map(new Func1<ServiceResponse<Void>, Void>() {
            @Override
            public Void call(ServiceResponse<Void> response) {
                return response.body();
            }
        });
    }

    /**
     * Deletes the specified compute policy from the specified Data Lake Analytics account.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param computePolicyName The name of the compute policy to delete.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceResponse} object if successful.
     */
    public Observable<ServiceResponse<Void>> deleteWithServiceResponseAsync(String resourceGroupName, String accountName, String computePolicyName) {
        if (this.client.subscriptionId() == null) {
            throw new IllegalArgumentException("Parameter this.client.subscriptionId() is required and cannot be null.");
        }
        if (resourceGroupName == null) {
            throw new IllegalArgumentException("Parameter resourceGroupName is required and cannot be null.");
        }
        if (accountName == null) {
            throw new IllegalArgumentException("Parameter accountName is required and cannot be null.");
        }
        if (computePolicyName == null) {
            throw new IllegalArgumentException("Parameter computePolicyName is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        return service.delete(this.client.subscriptionId(), resourceGroupName, accountName, computePolicyName, this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent())
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponse<Void>>>() {
                @Override
                public Observable<ServiceResponse<Void>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponse<Void> clientResponse = deleteDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    private ServiceResponse<Void> deleteDelegate(Response<ResponseBody> response) throws CloudException, IOException, IllegalArgumentException {
        return this.client.restClient().responseBuilderFactory().<Void, CloudException>newInstance(this.client.serializerAdapter())
                .register(200, new TypeToken<Void>() { }.getType())
                .register(204, new TypeToken<Void>() { }.getType())
                .registerError(CloudException.class)
                .build(response);
    }

    /**
     * Lists the Data Lake Analytics compute policies within the specified Data Lake Analytics account. An account supports, at most, 50 policies.
     *
     * @param nextPageLink The NextLink from the previous successful call to List operation.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the PagedList&lt;ComputePolicy&gt; object if successful.
     */
    public PagedList<ComputePolicy> listByAccountNext(final String nextPageLink) {
        ServiceResponse<Page<ComputePolicy>> response = listByAccountNextSinglePageAsync(nextPageLink).toBlocking().single();
        return new PagedList<ComputePolicy>(response.body()) {
            @Override
            public Page<ComputePolicy> nextPage(String nextPageLink) {
                return listByAccountNextSinglePageAsync(nextPageLink).toBlocking().single().body();
            }
        };
    }

    /**
     * Lists the Data Lake Analytics compute policies within the specified Data Lake Analytics account. An account supports, at most, 50 policies.
     *
     * @param nextPageLink The NextLink from the previous successful call to List operation.
     * @param serviceFuture the ServiceFuture object tracking the Retrofit calls
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<List<ComputePolicy>> listByAccountNextAsync(final String nextPageLink, final ServiceFuture<List<ComputePolicy>> serviceFuture, final ListOperationCallback<ComputePolicy> serviceCallback) {
        return AzureServiceFuture.fromPageResponse(
            listByAccountNextSinglePageAsync(nextPageLink),
            new Func1<String, Observable<ServiceResponse<Page<ComputePolicy>>>>() {
                @Override
                public Observable<ServiceResponse<Page<ComputePolicy>>> call(String nextPageLink) {
                    return listByAccountNextSinglePageAsync(nextPageLink);
                }
            },
            serviceCallback);
    }

    /**
     * Lists the Data Lake Analytics compute policies within the specified Data Lake Analytics account. An account supports, at most, 50 policies.
     *
     * @param nextPageLink The NextLink from the previous successful call to List operation.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the PagedList&lt;ComputePolicy&gt; object
     */
    public Observable<Page<ComputePolicy>> listByAccountNextAsync(final String nextPageLink) {
        return listByAccountNextWithServiceResponseAsync(nextPageLink)
            .map(new Func1<ServiceResponse<Page<ComputePolicy>>, Page<ComputePolicy>>() {
                @Override
                public Page<ComputePolicy> call(ServiceResponse<Page<ComputePolicy>> response) {
                    return response.body();
                }
            });
    }

    /**
     * Lists the Data Lake Analytics compute policies within the specified Data Lake Analytics account. An account supports, at most, 50 policies.
     *
     * @param nextPageLink The NextLink from the previous successful call to List operation.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the PagedList&lt;ComputePolicy&gt; object
     */
    public Observable<ServiceResponse<Page<ComputePolicy>>> listByAccountNextWithServiceResponseAsync(final String nextPageLink) {
        return listByAccountNextSinglePageAsync(nextPageLink)
            .concatMap(new Func1<ServiceResponse<Page<ComputePolicy>>, Observable<ServiceResponse<Page<ComputePolicy>>>>() {
                @Override
                public Observable<ServiceResponse<Page<ComputePolicy>>> call(ServiceResponse<Page<ComputePolicy>> page) {
                    String nextPageLink = page.body().nextPageLink();
                    if (nextPageLink == null) {
                        return Observable.just(page);
                    }
                    return Observable.just(page).concatWith(listByAccountNextWithServiceResponseAsync(nextPageLink));
                }
            });
    }

    /**
     * Lists the Data Lake Analytics compute policies within the specified Data Lake Analytics account. An account supports, at most, 50 policies.
     *
    ServiceResponse<PageImpl<ComputePolicy>> * @param nextPageLink The NextLink from the previous successful call to List operation.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the PagedList&lt;ComputePolicy&gt; object wrapped in {@link ServiceResponse} if successful.
     */
    public Observable<ServiceResponse<Page<ComputePolicy>>> listByAccountNextSinglePageAsync(final String nextPageLink) {
        if (nextPageLink == null) {
            throw new IllegalArgumentException("Parameter nextPageLink is required and cannot be null.");
        }
        String nextUrl = String.format("%s", nextPageLink);
        return service.listByAccountNext(nextUrl, this.client.acceptLanguage(), this.client.userAgent())
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponse<Page<ComputePolicy>>>>() {
                @Override
                public Observable<ServiceResponse<Page<ComputePolicy>>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponse<PageImpl<ComputePolicy>> result = listByAccountNextDelegate(response);
                        return Observable.just(new ServiceResponse<Page<ComputePolicy>>(result.body(), result.response()));
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    private ServiceResponse<PageImpl<ComputePolicy>> listByAccountNextDelegate(Response<ResponseBody> response) throws CloudException, IOException, IllegalArgumentException {
        return this.client.restClient().responseBuilderFactory().<PageImpl<ComputePolicy>, CloudException>newInstance(this.client.serializerAdapter())
                .register(200, new TypeToken<PageImpl<ComputePolicy>>() { }.getType())
                .registerError(CloudException.class)
                .build(response);
    }

}
