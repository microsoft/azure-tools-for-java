/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common.errorresponse;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.http.Header;
import org.apache.http.HttpEntity;

public class UnclassifiedHttpErrorStatus extends HttpErrorStatus {
    public UnclassifiedHttpErrorStatus(
            int statusCode,
            @NotNull String message,
            @Nullable Header[] headers,
            @Nullable HttpEntity entity) {
        super(statusCode, message, headers, entity);
    }
}
