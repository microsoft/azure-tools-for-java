/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;

public abstract class AuthenticationErrorHandler<T> {
    public final static int AUTH_ERROR_CODE = 401;

    public abstract T execute(String response);

    // add more logic for authentication error
    public T run(String response)throws AuthenticationException {
        T result = execute(response);
        if (result == null) {
               Type errorType = new TypeToken<AuthenticationError>() {}.getType();
               AuthenticationError authenticationError = new Gson().fromJson(response, errorType);
                if(authenticationError != null && authenticationError.getErrorDetail() != null) {
                    throw new AuthenticationException(authenticationError.getError(), AUTH_ERROR_CODE);
                }
           }

        return result;
    }
}
