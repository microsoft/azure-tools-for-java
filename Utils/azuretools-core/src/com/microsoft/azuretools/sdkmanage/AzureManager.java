/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.sdkmanage;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.implementation.InsightsManager;
import com.microsoft.azure.management.resources.Tenant;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.utils.Pair;

import java.io.IOException;
import java.util.List;

public interface AzureManager {
    Azure getAzure(String sid);

    Subscription getSubscriptionById(String sid);

    InsightsManager getInsightsManager(String sid);

    List<Subscription> getSubscriptions();

    List<Subscription> getSelectedSubscriptions();

    default List<String> getSelectedSubscriptionIds() {
        return null;
    }

    List<Pair<Subscription, Tenant>> getSubscriptionsWithTenant();

    SubscriptionManager getSubscriptionManager();

    void drop();

    String getCurrentUserId();

    String getAccessToken(String tid, String resource, PromptBehavior promptBehavior) throws IOException;

    String getManagementURI();

    String getStorageEndpointSuffix();

    String getScmSuffix();

    Environment getEnvironment();

    String getPortalUrl();

    default String getAccessToken(String tid) throws IOException {
        return getAccessToken(tid, CommonSettings.getAdEnvironment().resourceManagerEndpoint(), PromptBehavior.Auto);
    }
}
