/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.springcloud;

import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;

public class SpringCloudIdHelper {
    public static String getSubscriptionId(String serviceId) {
        return AzureMvpModel.getSegment(serviceId, "subscriptions");
    }

    public static String getResourceGroup(String serviceId) {
        return AzureMvpModel.getSegment(serviceId, "resourceGroups");
    }

    public static String getClusterName(String serviceId) {
        return AzureMvpModel.getSegment(serviceId, "Spring");
    }

    public static String getAppName(String serviceId) {
        return AzureMvpModel.getSegment(serviceId, "apps");
    }
}
