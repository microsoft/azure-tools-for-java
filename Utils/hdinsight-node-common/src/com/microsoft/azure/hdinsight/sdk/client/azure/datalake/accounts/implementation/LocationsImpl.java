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
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.Locations;
import com.google.common.reflect.TypeToken;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.models.CapabilityInformation;
import com.microsoft.rest.ServiceCallback;
import com.microsoft.rest.ServiceFuture;
import com.microsoft.rest.ServiceResponse;
import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.Response;
import rx.functions.Func1;
import rx.Observable;

/**
 * An instance of this class provides access to all the operations defined
 * in Locations.
 */
public class LocationsImpl implements Locations {
    /** The Retrofit service to perform REST calls. */
    private LocationsService service;
    /** The service client containing this operation class. */
    private DataLakeAnalyticsAccountManagementClientImpl client;

    /**
     * Initializes an instance of LocationsImpl.
     *
     * @param retrofit the Retrofit instance built from a Retrofit Builder.
     * @param client the instance of the service client containing this operation class.
     */
    public LocationsImpl(Retrofit retrofit, DataLakeAnalyticsAccountManagementClientImpl client) {
        this.service = retrofit.create(LocationsService.class);
        this.client = client;
    }

    /**
     * The interface defining all the services for Locations to be
     * used by Retrofit to perform actually REST calls.
     */
    interface LocationsService {
        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.Locations getCapability" })
        @GET("subscriptions/{subscriptionId}/providers/Microsoft.DataLakeAnalytics/locations/{location}/capability")
        Observable<Response<ResponseBody>> getCapability(@Path("subscriptionId") String subscriptionId, @Path("location") String location, @Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Header("User-Agent") String userAgent);

    }

    /**
     * Gets subscription-level properties and limits for Data Lake Analytics specified by resource location.
     *
     * @param location The resource location without whitespace.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the CapabilityInformation object if successful.
     */
    public CapabilityInformation getCapability(String location) {
        return getCapabilityWithServiceResponseAsync(location).toBlocking().single().body();
    }

    /**
     * Gets subscription-level properties and limits for Data Lake Analytics specified by resource location.
     *
     * @param location The resource location without whitespace.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<CapabilityInformation> getCapabilityAsync(String location, final ServiceCallback<CapabilityInformation> serviceCallback) {
        return ServiceFuture.fromResponse(getCapabilityWithServiceResponseAsync(location), serviceCallback);
    }

    /**
     * Gets subscription-level properties and limits for Data Lake Analytics specified by resource location.
     *
     * @param location The resource location without whitespace.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the CapabilityInformation object
     */
    public Observable<CapabilityInformation> getCapabilityAsync(String location) {
        return getCapabilityWithServiceResponseAsync(location).map(new Func1<ServiceResponse<CapabilityInformation>, CapabilityInformation>() {
            @Override
            public CapabilityInformation call(ServiceResponse<CapabilityInformation> response) {
                return response.body();
            }
        });
    }

    /**
     * Gets subscription-level properties and limits for Data Lake Analytics specified by resource location.
     *
     * @param location The resource location without whitespace.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the CapabilityInformation object
     */
    public Observable<ServiceResponse<CapabilityInformation>> getCapabilityWithServiceResponseAsync(String location) {
        if (this.client.subscriptionId() == null) {
            throw new IllegalArgumentException("Parameter this.client.subscriptionId() is required and cannot be null.");
        }
        if (location == null) {
            throw new IllegalArgumentException("Parameter location is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        return service.getCapability(this.client.subscriptionId(), location, this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent())
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponse<CapabilityInformation>>>() {
                @Override
                public Observable<ServiceResponse<CapabilityInformation>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponse<CapabilityInformation> clientResponse = getCapabilityDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    private ServiceResponse<CapabilityInformation> getCapabilityDelegate(Response<ResponseBody> response) throws CloudException, IOException, IllegalArgumentException {
        return this.client.restClient().responseBuilderFactory().<CapabilityInformation, CloudException>newInstance(this.client.serializerAdapter())
                .register(200, new TypeToken<CapabilityInformation>() { }.getType())
                .register(404, new TypeToken<Void>() { }.getType())
                .registerError(CloudException.class)
                .build(response);
    }

}
