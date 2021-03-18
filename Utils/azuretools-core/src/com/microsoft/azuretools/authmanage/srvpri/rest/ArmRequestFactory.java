/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.rest;

import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.srvpri.exceptions.AzureArmException;
import com.microsoft.azuretools.authmanage.srvpri.exceptions.AzureException;
import com.microsoft.azuretools.sdkmanage.AzureManagerBase;

/**
 * Created by vlashch on 8/29/16.
 */
class ArmRequestFactory extends RequestFactoryBase {
    private final AzureManagerBase preAccessTokenAzureManager;

    public ArmRequestFactory(AzureManagerBase preAccessTokenAzureManager, String tenantId) {
        this.preAccessTokenAzureManager = preAccessTokenAzureManager;
        this.urlPrefix = CommonSettings.getAdEnvironment().resourceManagerEndpoint() + "subscriptions/";
        this.tenantId = tenantId;
        this.resource =  CommonSettings.getAdEnvironment().resourceManagerEndpoint();
        this.apiVersion = "api-version=2015-07-01";
    }

    @Override
    public AzureException newAzureException(String message) {
        return new AzureArmException(message);
    }

    @Override
    AzureManagerBase getPreAccessTokenAzureManager() {
        return this.preAccessTokenAzureManager;
    }
}
