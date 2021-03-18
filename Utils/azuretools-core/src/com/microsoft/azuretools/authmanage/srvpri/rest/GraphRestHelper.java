/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.rest;

import com.microsoft.azuretools.sdkmanage.AzureManagerBase;

public class GraphRestHelper extends RestHelperBase {

    public GraphRestHelper(AzureManagerBase preAccessTokenAzureManager, String tenantId) {
        setRequestFactory(new GraphRequestFactory(preAccessTokenAzureManager, tenantId));
    }
}
