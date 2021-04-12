/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common.errorresponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

import com.microsoft.azuretools.azurecommons.helpers.Nullable;

public class GatewayTimeoutErrorStatus extends HttpErrorStatus {
    public GatewayTimeoutErrorStatus(String message, @Nullable Header[] headers, @Nullable HttpEntity entity) {
        super(504, message, headers, entity);
    }
}
