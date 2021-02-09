/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.sdkmanage;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.implementation.InsightsManager;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.management.mysql.v2020_01_01.implementation.MySQLManager;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.Tenant;
import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.utils.Pair;

import java.io.IOException;
import java.util.List;

public interface AzureManager {
    Azure getAzure(String sid);

    AppPlatformManager getAzureSpringCloudClient(String sid);

    MySQLManager getMySQLManager(String sid);

    InsightsManager getInsightsManager(String sid);

    List<Subscription> getSubscriptions();

    List<Pair<Subscription, Tenant>> getSubscriptionsWithTenant();

    Settings getSettings();

    SubscriptionManager getSubscriptionManager();

    void drop();

    String getCurrentUserId();

    String getAccessToken(String tid, String resource, PromptBehavior promptBehavior) throws IOException;

    String getManagementURI();

    String getStorageEndpointSuffix();

    String getTenantIdBySubscription(String subscriptionId);

    String getScmSuffix();

    Environment getEnvironment();

    String getPortalUrl();

    default String getAccessToken(String tid) throws IOException {
        return getAccessToken(tid, CommonSettings.getAdEnvironment().resourceManagerEndpoint(), PromptBehavior.Auto);
    }
}
