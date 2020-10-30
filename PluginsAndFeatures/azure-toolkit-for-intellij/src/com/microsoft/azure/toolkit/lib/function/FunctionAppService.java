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
package com.microsoft.azure.toolkit.lib.function;

import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationInsightsComponent;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.toolkit.intellij.function.FunctionAppComboBoxModel;
import com.microsoft.azure.toolkit.lib.appservice.ApplicationInsightsConfig;
import com.microsoft.azuretools.telemetrywrapper.*;
import com.microsoft.intellij.runner.functions.deploy.FunctionDeployModel;
import com.microsoft.intellij.runner.functions.library.function.CreateFunctionHandler;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_FUNCTION_APP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.FUNCTION;


public class FunctionAppService {

    private static final String APP_INSIGHTS_INSTRUMENTATION_KEY = "APPINSIGHTS_INSTRUMENTATIONKEY";

    private static final FunctionAppService instance = new FunctionAppService();

    public static FunctionAppService getInstance() {
        return FunctionAppService.instance;
    }

    public FunctionApp createFunctionApp(final FunctionAppConfig config) throws Exception {
        final FunctionDeployModel functionDeployModel = new FunctionDeployModel();
        functionDeployModel.saveModel(new FunctionAppComboBoxModel(config));
        final Operation operation = TelemetryManager.createOperation(FUNCTION, CREATE_FUNCTION_APP);
        try {
            operation.start();
            final CreateFunctionHandler createFunctionHandler = new CreateFunctionHandler(functionDeployModel);
            return createFunctionHandler.execute();
        } catch (final Exception e) {
            throw e;
        } finally {
            operation.complete();
        }
    }

    private void bindingApplicationInsights(FunctionAppConfig config) throws IOException {
        if (config.getMonitorConfig()!= null && config.getMonitorConfig().getApplicationInsightsConfig() == null) {
            return;
        }
        final ApplicationInsightsConfig insightsConfig = config.getMonitorConfig().getApplicationInsightsConfig();
        String instrumentationKey = insightsConfig.getInstrumentationKey();
        if (StringUtils.isEmpty(instrumentationKey)) {
            final String region = config.getRegion().name();
            final String insightsName = config.getName();
            final ApplicationInsightsComponent insights =
                    AzureSDKManager.getOrCreateApplicationInsights(config.getSubscription().subscriptionId(),
                                                                   config.getResourceGroup().name(),
                                                                   insightsName,
                                                                   region);
            instrumentationKey = insights.instrumentationKey();
        }
        config.getAppSettings().put(APP_INSIGHTS_INSTRUMENTATION_KEY, instrumentationKey);
    }
}
