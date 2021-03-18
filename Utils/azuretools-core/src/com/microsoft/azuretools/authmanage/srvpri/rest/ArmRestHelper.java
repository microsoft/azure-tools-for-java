/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.rest;

import com.microsoft.azuretools.sdkmanage.AzureManagerBase;

/**
 * Created by vlashch on 8/29/16.
 */
public class ArmRestHelper extends RestHelperBase {

    public ArmRestHelper(AzureManagerBase preAccessTokenAzureManager, String tenantId) {
        setRequestFactory(new ArmRequestFactory(preAccessTokenAzureManager, tenantId));
    }
}
