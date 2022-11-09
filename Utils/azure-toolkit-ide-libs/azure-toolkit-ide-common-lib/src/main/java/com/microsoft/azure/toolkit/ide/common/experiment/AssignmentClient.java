/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.experiment;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.*;


public class AssignmentClient {
    private static final AssignmentClient instance = new AssignmentClient();
    private static final String endPoint = "https://default.exp-tas.com/exptas76/80fa735a-bc58-43ba-8d32-835a83d727b9-intellijexp/api/v1/tas";
    private final String NAME_SPACE = "default";
    private final String ASSIGNMENT_UNIT_ID = "installationid";
    private final String AUDIENCE_FILTER_ID = "userstype";
    private final String AUDIENCE_FILTER_VALUE = "intellij";
    private final OkHttpClient client = new OkHttpClient();
    private final Request request;
    private final Map<String, String> featuresCache = new HashMap<>();
    private static final ObjectMapper JSON_MAPPER = new JsonMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public AssignmentClient() {
        request = new Request.Builder()
                .url(endPoint)
                .addHeader("x-exp-sdk-version", "Microsoft.VariantAssignment.Client/1.0.0")
                .addHeader("x-exp-parameters", String.format("%s=%s,%s=%s", ASSIGNMENT_UNIT_ID,
                        InstallationIdUtils.getHashMac(), AUDIENCE_FILTER_ID, AUDIENCE_FILTER_VALUE))
                .build();
    }

    public static AssignmentClient getInstance() {
        return instance;
    }

    public void updateFeatures() {
        try {
            final Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                final AssignmentResponse assignmentResponse = JSON_MAPPER.readValue(response.body().byteStream(), AssignmentResponse.class);
                final List<AssignmentResponse.Config> configList = assignmentResponse.getConfigs();
                configList.forEach(config -> {
                    if (Objects.equals(config.getId(), NAME_SPACE)) {
                        featuresCache.putAll(config.getParameters());
                    }
                });
                featuresCache.put(FeatureFlag.ASSIGNMENT_CONTEXT.getFlagName(), assignmentResponse.getAssignmentContext());
            }
        } catch (final Exception e) {
            throw new AzureToolkitRuntimeException(e);
        }
    }

    @Nonnull
    public String getFeatureVariable(String featureFlagName) {
        return Optional.ofNullable(featuresCache.get(featureFlagName)).orElse(StringUtils.EMPTY);
    }

    @Nonnull
    public String getFeatureVariableAsync(String featureFlagName) {
        updateFeatures();
        return Optional.ofNullable(featuresCache.get(featureFlagName)).orElse(StringUtils.EMPTY);
    }

}
