/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.application.ApplicationManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.Application;
import com.microsoft.graph.models.ApplicationAddPasswordParameterSet;
import com.microsoft.graph.models.Domain;
import com.microsoft.graph.models.PasswordCredential;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

final class AzureUtils {
    private AzureUtils() {
    }

    /**
     * Creates a new client password credential for the given application.
     *
     * @param client      The Microsoft Graph client
     * @param application The application to associate with the new credentials.
     * @return The new credentials.
     * @throws com.microsoft.graph.core.ClientException If the credentials could not be created
     */
    @NotNull
    static PasswordCredential createApplicationClientSecret(@NotNull GraphServiceClient<Request> client,
                                                            @NotNull Application application) {
        assert application.id != null;

        var credential = new PasswordCredential();
        var secret = ApplicationAddPasswordParameterSet.newBuilder().withPasswordCredential(credential).build();
        return client.applications(application.id)
                .addPassword(secret)
                .buildRequest()
                .post();
    }

    /**
     * Synchronously loads all domains by iterating all available pages.
     *
     * @param client The Graph client
     * @return The list of applications
     */
    @NotNull
    static List<Domain> loadDomains(@NotNull GraphServiceClient<Request> client) {
        var domains = new ArrayList<Domain>();

        var request = client.domains().buildRequest();
        while (true) {
            var page = request.get();
            if (page == null) {
                break;
            }
            domains.addAll(page.getCurrentPage());

            var nextPage = page.getNextPage();
            if (nextPage == null) {
                break;
            }
            request = nextPage.buildRequest();
        }

        return domains;
    }

    /**
     * Synchronously loads all applications by iterating all available pages.
     *
     * @param client The Graph client
     * @return The list of applications
     */
    @NotNull
    static List<Application> loadApplications(@NotNull GraphServiceClient<Request> client) {
        var applications = new ArrayList<Application>();

        var request = client.applications().buildRequest();
        while (true) {
            var page = request.get();
            if (page == null) {
                break;
            }
            applications.addAll(page.getCurrentPage());

            var nextPage = page.getNextPage();
            if (nextPage == null) {
                break;
            }
            request = nextPage.buildRequest();
        }

        return applications;
    }

    static GraphServiceClient<Request> createGraphClient(@NotNull Subscription subscription) {
        var account = Azure.az(AzureAccount.class).account();
        var credentials = account.getTokenCredential(subscription.getId());
        var authProvider = new TokenCredentialAuthProvider(credentials);

        return GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
    }
}
