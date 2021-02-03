/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.rest.RestException;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Exception thrown for an invalid response with ErrorContract information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorContractException extends RestException {
    /**
     * Initializes a new instance of the ErrorContractException class.
     *
     * @param message the exception message or the response content if a message is not available
     * @param response the HTTP response
     */
    public ErrorContractException(final String message, final Response<ResponseBody> response) {
        super(message, response);
    }

    /**
     * Initializes a new instance of the ErrorContractException class.
     *
     * @param message the exception message or the response content if a message is not available
     * @param response the HTTP response
     * @param body the deserialized response body
     */
    public ErrorContractException(final String message, final Response<ResponseBody> response, final ErrorContract body) {
        super(message, response, body);
    }

    @Override
    public ErrorContract body() {
        return (ErrorContract) super.body();
    }
}
