/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.intellij;

import com.intellij.openapi.application.ApplicationInfo;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.azuretools.telemetry.AppInsightsConfiguration;
import com.microsoft.intellij.ui.messages.AzureBundle;
import com.microsoft.intellij.util.PluginHelper;

import java.io.File;
import java.util.UUID;

public class AppInsightsConfigurationImpl implements AppInsightsConfiguration {
    static final String EVENT_NAME_PREFIX_INTELLIJ = "AzurePlugin.Intellij.";
    // eventname for new telemetry
    static final String EVENT_NAME = "AzurePlugin.Intellij";
    static final String sessionId = UUID.randomUUID().toString();
    static final String dataFile = PluginHelper.getTemplateFile(AzureBundle.message("dataFileName"));
    static final String ide = getIDE();

    private static final String getIDE() {
        ApplicationInfo info = ApplicationInfo.getInstance();
        return String.format("%s_%s_%s", info.getVersionName(), info.getFullVersion(), info.getBuild());
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    @Override
    public String pluginVersion() {
        return DataOperations.getProperty(dataFile, AzureBundle.message("pluginVersion"));
    }

    @Override
    public String installationId() {
        return DataOperations.getProperty(dataFile, AzureBundle.message("instID"));
    }

    @Override
    public String preferenceVal() {
        return DataOperations.getProperty(dataFile, AzureBundle.message("prefVal"));
    }

    @Override
    public boolean validated() {
        return new File(dataFile).exists();
    }

    @Override
    public String eventNamePrefix() {
        return EVENT_NAME_PREFIX_INTELLIJ;
    }

    @Override
    public String ide() {
        return ide;
    }

    @Override
    public String eventName() {
        return EVENT_NAME;
    }

}
