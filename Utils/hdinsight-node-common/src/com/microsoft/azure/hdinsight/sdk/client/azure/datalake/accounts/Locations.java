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

package com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.models.CapabilityInformation;
import com.microsoft.rest.ServiceCallback;
import com.microsoft.rest.ServiceFuture;
import com.microsoft.rest.ServiceResponse;
import java.io.IOException;
import rx.Observable;

/**
 * An instance of this class provides access to all the operations defined
 * in Locations.
 */
public interface Locations {
    /**
     * Gets subscription-level properties and limits for Data Lake Analytics specified by resource location.
     *
     * @param location The resource location without whitespace.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the CapabilityInformation object if successful.
     */
    CapabilityInformation getCapability(String location);

    /**
     * Gets subscription-level properties and limits for Data Lake Analytics specified by resource location.
     *
     * @param location The resource location without whitespace.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    ServiceFuture<CapabilityInformation> getCapabilityAsync(String location, final ServiceCallback<CapabilityInformation> serviceCallback);

    /**
     * Gets subscription-level properties and limits for Data Lake Analytics specified by resource location.
     *
     * @param location The resource location without whitespace.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the CapabilityInformation object
     */
    Observable<CapabilityInformation> getCapabilityAsync(String location);

    /**
     * Gets subscription-level properties and limits for Data Lake Analytics specified by resource location.
     *
     * @param location The resource location without whitespace.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the CapabilityInformation object
     */
    Observable<ServiceResponse<CapabilityInformation>> getCapabilityWithServiceResponseAsync(String location);

}
