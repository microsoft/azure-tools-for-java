/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.applicationinsights.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ApplicationInsightsResourceDefinition extends AzureServiceResource.Definition<ApplicationInsight> {
    public static final ApplicationInsightsResourceDefinition INSTANCE = new ApplicationInsightsResourceDefinition();

    public ApplicationInsightsResourceDefinition() {
        super("Azure.Insights", "Azure Application Insights", AzureIcons.ApplicationInsights.MODULE.getIconPath());
    }

    @Override
    public ApplicationInsight getResource(String dataId, final String id) {
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
        env.put("ApplicationInsightsAgent_EXTENSION_VERSION", "~2"); // todo: set value by target resource, should be `~3` for linux app service
        return env;
    }

    @Override
    public AzureFormJPanel<Resource<ApplicationInsight>> getResourcePanel(Project project) {
        return new ApplicationInsightsResourcePanel();
    }

}
