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

package com.microsoft.intellij.util;

import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.runner.webapp.webappconfig.IntelliJWebAppSettingModel;
import java.util.HashMap;
import java.util.Map;

public class TelemetryUtil {
    public static void sendTelemetryOpStart(String productionName, String operationName) {
        TelemetryManager.getInstance().getProducer().startTransaction(productionName, operationName);
    }

    public static void sendTelemetryOpEnd() {
        TelemetryManager.getInstance().getProducer().endTransaction();
    }

    public static void sendTelemetryOpError(ErrorType errorType, String errMsg, Map<String, String> properties) {
        TelemetryManager.getInstance().getProducer().sendError(errorType, errMsg, properties, null);
    }

    public static void sendTelemetryInfo(Map<String, String> properties) {
        TelemetryManager.getInstance().getProducer().sendInfo(properties, null);
    }

    public static Map<String, String> buildProperties(Map<String, String> properties,
        IntelliJWebAppSettingModel webAppSettingModel) {
        Map<String, String> result = new HashMap<>();
        try {
            if (properties != null) {
                result.putAll(properties);
            }
            result.put(TelemetryConstants.RUNTIME,
                webAppSettingModel.getOS() == OperatingSystem.LINUX ? "linux-" + webAppSettingModel.getLinuxRuntime()
                    .toString() : "windows-" + webAppSettingModel.getWebContainer());
            result.put(TelemetryConstants.WEBAPP_DEPLOY_TO_SLOT, String.valueOf(webAppSettingModel.isDeployToSlot()));
            result.put(TelemetryConstants.SUBSCRIPTIONID, webAppSettingModel.getSubscriptionId());
            result.put(TelemetryConstants.CREATE_NEWWEBAPP, String.valueOf(webAppSettingModel.isCreatingNew()));
            result.put(TelemetryConstants.CREATE_NEWASP, String.valueOf(webAppSettingModel.isCreatingAppServicePlan()));
            result.put(TelemetryConstants.CREATE_NEWRG, String.valueOf(webAppSettingModel.isCreatingResGrp()));
            result.put(TelemetryConstants.FILETYPE, MavenRunTaskUtil.getFileType(webAppSettingModel.getTargetName()));
        } catch (Exception ignore) {
        }
        return result;
    }
}