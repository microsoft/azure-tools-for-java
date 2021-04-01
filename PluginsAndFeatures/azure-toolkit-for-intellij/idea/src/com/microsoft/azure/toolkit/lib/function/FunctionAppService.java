/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.function;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.toolkit.intellij.function.FunctionAppComboBoxModel;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.azure.toolkit.intellij.function.runner.deploy.FunctionDeployModel;
import com.microsoft.azure.toolkit.intellij.function.runner.library.function.CreateFunctionHandler;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_FUNCTION_APP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.FUNCTION;


public class FunctionAppService {

    private static final FunctionAppService instance = new FunctionAppService();

    public static FunctionAppService getInstance() {
        return FunctionAppService.instance;
    }

    public FunctionApp createFunctionApp(final FunctionAppConfig config) {
        final Operation operation = TelemetryManager.createOperation(FUNCTION, CREATE_FUNCTION_APP);
        try {
            operation.start();
            final FunctionDeployModel functionDeployModel = new FunctionDeployModel();
            functionDeployModel.saveModel(new FunctionAppComboBoxModel(config));
            final CreateFunctionHandler createFunctionHandler = new CreateFunctionHandler(functionDeployModel);
            return createFunctionHandler.execute();
        } finally {
            operation.complete();
        }
    }
}
