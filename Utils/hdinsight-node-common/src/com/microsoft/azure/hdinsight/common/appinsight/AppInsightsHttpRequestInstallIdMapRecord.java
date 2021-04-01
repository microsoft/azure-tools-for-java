/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common.appinsight;

import com.microsoft.azuretools.telemetry.AppInsightsClient;

import java.util.HashMap;

public class AppInsightsHttpRequestInstallIdMapRecord {
    private final String requestId;
    private final String installId;

    public AppInsightsHttpRequestInstallIdMapRecord(String requestId, String installId) {
        this.requestId = requestId;
        this.installId = installId;
    }

    public void post() {
        // Create the relationship of UUID <-> InstallationID in App Insight
        HashMap<String, String> mapEventProps = new HashMap<String, String>() {{
            put("RequestId", requestId);
            put("InstallId", installId);
        }};

        try {
            AppInsightsClient.create(HDInsightCommonBundle.message("HDInsightRequestInstallIdMap"), null, mapEventProps);
        } catch (Exception ignored) {}
    }
}
