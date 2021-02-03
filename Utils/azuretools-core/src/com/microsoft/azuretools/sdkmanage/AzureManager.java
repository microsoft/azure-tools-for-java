/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azuretools.sdkmanage;

import com.azure.core.credential.AccessToken;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.AzureResourceManager;
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
import org.parboiled.common.StringUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

public interface AzureManager {
    default AzureResourceManager getAzureResourceManager(String sid) {
        // Todo: replace with identity implement
        final AzureEnvironment azureEnvironment = AzureEnvironment.knownEnvironments().stream()
                .filter(environment -> StringUtils.equalsIgnoreCase(environment.getManagementEndpoint(),
                        getEnvironment().getAzureEnvironment().managementEndpoint()))
                .findFirst().orElse(AzureEnvironment.AZURE);
        final AzureProfile azureProfile = new AzureProfile(azureEnvironment);
        return AzureResourceManager.authenticate(tokenRequestContext -> {
            try {
                final String token = getAccessToken(azureProfile.getEnvironment().getManagementEndpoint());
                return Mono.just(new AccessToken(token, OffsetDateTime.MAX));
            } catch (IOException e) {
                return Mono.error(e);
            }
        }, azureProfile).withSubscription(sid);
    }

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
