/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;
import com.microsoft.intellij.AzureAnAction;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QualtricsSurveyAction extends AzureAnAction {

    private static final String SURVEY_URL = "https://microsoft.qualtrics.com/jfe/form/SV_b17fG5QQlMhs2up?" +
            "toolkit=%s&ide=%s&os=%s&jdk=%s&id=%s";

    public QualtricsSurveyAction() {
        super("Provide Feedback");
    }

    @Override
    public boolean onActionPerformed(@NotNull AnActionEvent anActionEvent, @Nullable Operation operation) {
        BrowserUtil.browse(getRequestUrl());
        return true;
    }

    @Override
    protected String getServiceName(AnActionEvent event) {
        return TelemetryConstants.SYSTEM;
    }

    @Override
    protected String getOperationName(AnActionEvent event) {
        return TelemetryConstants.FEEDBACK;
    }

    private String getRequestUrl() {
        IdeaPluginDescriptor pluginDescriptor = PluginManager
                .getPlugin(PluginId.getId("com.microsoft.tooling.msservices.intellij.azure"));
        ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
        String toolkit = pluginDescriptor.getVersion();
        String ide = String.format("%s %s", applicationInfo.getFullVersion(), applicationInfo.getBuild());
        String os = System.getProperty("os.name");
        String jdk = String.format("%s %s", System.getProperty("java.vendor"), System.getProperty("java.version"));
        String id = AppInsightsClient.getInstallationId();
        return String.format(SURVEY_URL, toolkit, ide, os, jdk, id);
    }
}
