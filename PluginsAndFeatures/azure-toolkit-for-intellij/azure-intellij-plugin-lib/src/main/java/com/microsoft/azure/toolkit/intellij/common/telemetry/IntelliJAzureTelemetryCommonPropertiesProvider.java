/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.telemetry;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.PermanentInstallationID;
import com.microsoft.azure.toolkit.intellij.common.CommonConst;
import com.microsoft.azure.toolkit.intellij.common.settings.IntellijStore;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetryConfigProvider;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer.TELEMETRY_INSTALLATION_ID;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.PROXY;

public class IntelliJAzureTelemetryCommonPropertiesProvider implements AzureTelemetryConfigProvider {
    @Override
    public Map<String, String> getCommonProperties() {
        final Map<String, String> properties = new HashMap<>();
        final ApplicationInfo info = ApplicationInfo.getInstance();
        properties.put("SessionId", UUID.randomUUID().toString());
        properties.put("IDE", String.format("%s_%s_%s", info.getVersionName(), info.getFullVersion(), info.getBuild()));
//        properties.put("AssignmentContext", Optional.ofNullable(ExperimentationClient.getExperimentationService()).map(ExperimentationService::getAssignmentContext).orElse(StringUtils.EMPTY));
        properties.put("Plugin Version", CommonConst.PLUGIN_VERSION);
        properties.put("Installation ID", getInstallationId());
        if (StringUtils.isNotBlank(Azure.az().config().getProxySource())) {
            properties.put(PROXY, "true");
        }
        return properties;
    }

    // workaround to fix telemeter was initialized before plugin life cycle listener executed
    public static final String getInstallationId() {
        String installId = IntellijStore.getInstance().getProperty(null, TELEMETRY_INSTALLATION_ID);

        if (StringUtils.isBlank(installId) || !InstallationIdUtils.isValidHashMac(installId)) {
            installId = InstallationIdUtils.getHashMac();
        }
        if (StringUtils.isBlank(installId) || !InstallationIdUtils.isValidHashMac(installId)) {
            installId = InstallationIdUtils.hash(PermanentInstallationID.get());
        }
        return installId;
    }

    @Override
    public String getEventNamePrefix() {
        return "AzurePlugin.Intellij";
    }

}
