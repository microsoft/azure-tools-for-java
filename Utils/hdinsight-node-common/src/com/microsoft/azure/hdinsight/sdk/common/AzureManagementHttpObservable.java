/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public class AzureManagementHttpObservable extends AzureHttpObservable {
    public AzureManagementHttpObservable(@NotNull Subscription subscription, @NotNull String apiVersion) {
        super(subscription, apiVersion);
    }

    @NotNull
    @Override
    public String getResourceEndpoint() {
        String endpoint = CommonSettings.getAdEnvironment().managementEndpoint();

        return endpoint != null ? endpoint : "https://management.core.windows.net/";
    }
}
