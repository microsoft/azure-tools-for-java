/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.rest;

import com.microsoft.azuretools.authmanage.srvpri.exceptions.AzureException;

import java.io.IOException;

/**
 * Created by vlashch on 8/29/16.
 */

interface IRequestFactory {
    String getApiVersion();
    String getUrlPattern();
    String getUrlPatternParamless();
    String getUrlPrefix();
    String getAccessToken() throws IOException;
    AzureException newAzureException(String message);
}
