/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;
import com.microsoft.azuretools.ijidea.utility.AzureAnAction;
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
