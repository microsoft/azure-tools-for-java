/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.applicationinsights.connection;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.CommonConst;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.IJavaAgentSupported;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import com.microsoft.azure.toolkit.lib.common.cache.Preload;
import lombok.Getter;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ApplicationInsightsResourceDefinition extends AzureServiceResource.Definition<ApplicationInsight> implements IJavaAgentSupported {
    public static final ApplicationInsightsResourceDefinition INSTANCE = new ApplicationInsightsResourceDefinition();

    public ApplicationInsightsResourceDefinition() {
        super("Azure.Insights", "Azure Application Insights", AzureIcons.ApplicationInsights.MODULE.getIconPath());
    }

    @Override
    public ApplicationInsight getResource(String dataId) {
        return Azure.az(AzureApplicationInsights.class).getById(dataId);
    }

    @Override
    public List<Resource<ApplicationInsight>> getResources(Project project) {
        return Azure.az(AzureApplicationInsights.class).list().stream()
            .flatMap(m -> m.getApplicationInsightsModule().list().stream())
            .map(this::define).toList();
    }

    @Override
    public Map<String, String> initEnv(AzureServiceResource<ApplicationInsight> data, Project project) {
        final ApplicationInsight insight = data.getData();
        final HashMap<String, String> env = new HashMap<>();
        env.put("APPINSIGHTS_INSTRUMENTATIONKEY", insight.getInstrumentationKey());
        env.put("APPLICATIONINSIGHTS_CONNECTION_STRING", insight.getConnectionString());
        return env;
    }

    @Override
    public AzureFormJPanel<Resource<ApplicationInsight>> getResourcePanel(Project project) {
        return new ApplicationInsightsResourcePanel();
    }

    @Override
    @Nullable
    public File getJavaAgent() {
        return ApplicationInsightsAgentHolder.getApplicationInsightsLibrary();
    }

    // todo: @hanli
    //      1. Get latest ai library release
    //      2. Framework for plugin local file cache
    static class ApplicationInsightsAgentHolder {
        private static final String APPLICATION_INSIGHTS_URL =
            "https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.4.10/applicationinsights-agent-3.4.10.jar";
        private static final File applicationInsightsLibrary =
            new File(PluginManagerCore.getPlugin(PluginId.findId(CommonConst.PLUGIN_ID)).getPluginPath().toString(), "applicationinsights-agent.jar");

        @Preload
        public static synchronized File getApplicationInsightsLibrary() {
            if (!applicationInsightsLibrary.exists()) {
                try {
                    FileUtils.copyURLToFile(new URL(APPLICATION_INSIGHTS_URL), applicationInsightsLibrary);
                } catch (IOException e) {
                    return null;
                }
            }
            return applicationInsightsLibrary;
        }
    }
}
