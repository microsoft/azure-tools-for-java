/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.authorization.fluent.GraphRbacManagementClient;
import com.azure.resourcemanager.authorization.implementation.GraphRbacManagementClientBuilder;
import com.azure.resourcemanager.resources.fluentcore.utils.HttpPipelineProvider;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.jetbrains.annotations.NotNull;

final class AzureUtils {
    private AzureUtils() {
    }

    @NotNull
    static GraphRbacManagementClient createGraphClient(@NotNull Subscription subscription) {
        var account = Azure.az(AzureAccount.class).account();
        var baseUrl = account.getEnvironment().getGraphEndpoint();
        var credentials = account.getTokenCredential(subscription.getId());
        return new GraphRbacManagementClientBuilder()
                .environment(account.getEnvironment())
                .endpoint(baseUrl)
                .tenantId(subscription.getTenantId())
                .pipeline(HttpPipelineProvider.buildHttpPipeline(credentials, new AzureProfile(account.getEnvironment())))
                .buildClient();
    }

    static boolean isLoggedOut() {
        try {
            Azure.az(AzureAccount.class).account();
            return false;
        } catch (AzureToolkitAuthenticationException ignored) {
            return true;
        }
    }
}
