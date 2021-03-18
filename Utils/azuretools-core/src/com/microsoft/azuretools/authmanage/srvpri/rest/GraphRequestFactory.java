/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.rest;

/**
 * Created by vlashch on 8/29/16.
 */


import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.srvpri.exceptions.AzureException;
import com.microsoft.azuretools.authmanage.srvpri.exceptions.AzureGraphException;
import com.microsoft.azuretools.sdkmanage.AzureManagerBase;

public class GraphRequestFactory extends RequestFactoryBase {
    private final AzureManagerBase preAccessTokenAzureManager;

    public GraphRequestFactory(AzureManagerBase preAccessTokenAzureManager, String tenantId) {
        this.preAccessTokenAzureManager = preAccessTokenAzureManager;
        this.tenantId = tenantId;
        this.urlPrefix = CommonSettings.getAdEnvironment().graphEndpoint() + this.tenantId + "/";
        this.resource =  CommonSettings.getAdEnvironment().graphEndpoint();
        apiVersion = "api-version=1.6";
    }

    @Override
    public AzureException newAzureException(String message) {
        return new AzureGraphException(message);
    }

    @Override
    AzureManagerBase getPreAccessTokenAzureManager() {
        return preAccessTokenAzureManager;
    }
}
