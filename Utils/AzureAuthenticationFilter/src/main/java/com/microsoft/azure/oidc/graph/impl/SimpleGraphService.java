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

package com.microsoft.azure.oidc.graph.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.oidc.application.settings.ApplicationSettings;
import com.microsoft.azure.oidc.application.settings.ApplicationSettingsLoader;
import com.microsoft.azure.oidc.application.settings.Secret;
import com.microsoft.azure.oidc.application.settings.Tenant;
import com.microsoft.azure.oidc.application.settings.impl.SimpleApplicationSettingsLoader;
import com.microsoft.azure.oidc.common.id.ID;
import com.microsoft.azure.oidc.exception.GeneralException;
import com.microsoft.azure.oidc.graph.GraphService;

public final class SimpleGraphService implements GraphService {
    private static final GraphService INSTANCE = new SimpleGraphService();
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleGraphService.class);

    private final ApplicationSettingsLoader applicationSettingsLoader = SimpleApplicationSettingsLoader.getInstance();

    @Override
    public Future<Boolean> isUserInRoleAsync(final String userID, final String role) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Future<Boolean> future = executorService.submit(new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return isUserInRole(userID, role);
            }
        });
        executorService.shutdown();
        return future;
    }

    private Boolean isUserInRole(final String userID, final String role) {
        try {
            final ApplicationSettings applicationSettings = applicationSettingsLoader.load();
            final String bearerToken = getBearerToken(applicationSettings.getTenant(),
                    applicationSettings.getPrincipalId(), applicationSettings.getPrincipalSecret());
            final String roleID = getGroupID(applicationSettings.getTenant(), role, bearerToken);
            if (roleID == null) {
                return Boolean.FALSE;
            }
            return isUserInGroup(applicationSettings.getTenant(), userID, roleID, bearerToken);
        } catch (GeneralException e) {
            LOGGER.error("General Exception", e);
            return Boolean.FALSE;
        } catch (RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
            return Boolean.FALSE;
        }
    }

    private Boolean isUserInGroup(final Tenant tenant, final String userID, final String groupID,
            final String bearerToken) {
        try {
            final String urlString = String.format("https://graph.windows.net/%s/isMemberOf?api-version=1.6",
                    tenant.getName());
            final String payload = String.format("{\"groupId\":\"%s\",\"memberId\":\"%s\"}", groupID, userID);
            final URL url = new URL(urlString);
            final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestProperty("Host", "graph.windows.net");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.getOutputStream().write(payload.getBytes());
            connection.getOutputStream().flush();
            final StringBuilder result = new StringBuilder();
            try (final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    result.append(line);
                }
            }
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode node = mapper.readValue(result.toString().getBytes(), JsonNode.class);
            return node.get("value").asBoolean();
        } catch (IOException e) {
            LOGGER.error("IO Exception", e);
            return Boolean.FALSE;
        } catch (RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
            return Boolean.FALSE;
        }
    }

    private String getGroupID(final Tenant tenant, final String group, final String bearerToken) {
        try {
            final String urlString = String.format(
                    "https://graph.windows.net/%s/groups?$filter=displayName%%20eq%%20'%s'&api-version=1.6",
                    tenant.getName(), URLEncoder.encode(group, "UTF-8"));
            final URL url = new URL(urlString);
            final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestProperty("Host", "graph.windows.net");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
            final StringBuilder result = new StringBuilder();
            try (final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    result.append(line);
                }
            }
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode node = mapper.readValue(result.toString().getBytes(), JsonNode.class);
            for (final JsonNode groupNode : node.get("value")) {
                return groupNode.get("objectId").asText();
            }
            return null;
        } catch (IOException e) {
            LOGGER.error("IO Exception", e);
            return null;
        } catch (RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    private String getBearerToken(final Tenant tenant, final ID principal, final Secret secret) {
        try {
            final String payload = String.format(
                    "grant_type=client_credentials&client_id=%s&client_secret=%s&resource=%s", principal.getValue(),
                    URLEncoder.encode(secret.getValue(), "UTF-8"),
                    URLEncoder.encode("https://graph.windows.net", "UTF-8"));
            final URL url = new URL(
                    String.format("https://login.microsoftonline.com/%s/oauth2/token", tenant.getName()));
            final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Host", "login.microsoftonline.com");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.getOutputStream().write(payload.getBytes());
            connection.getOutputStream().flush();
            final StringBuilder result = new StringBuilder();
            try (final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    result.append(line);
                }
            }
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode node = mapper.readValue(result.toString().getBytes(), JsonNode.class);
            return node.get("access_token").asText();
        } catch (IOException e) {
            LOGGER.error("IO Exception", e);
            return null;
        } catch (RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    public static GraphService getInstance() {
        return INSTANCE;
    }
}
