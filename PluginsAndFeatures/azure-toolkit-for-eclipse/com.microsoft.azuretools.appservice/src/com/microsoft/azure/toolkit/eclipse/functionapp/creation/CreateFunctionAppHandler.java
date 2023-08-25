/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.functionapp.creation;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

public class CreateFunctionAppHandler {

    public static void create() {
        Shell shell = Display.getCurrent().getActiveShell();
        final FunctionAppConfig defaultConfig = FunctionAppConfig.getFunctionAppDefaultConfig();
        defaultConfig.setRuntime(CreateFunctionAppDialog.FUNCTION_WINDOWS_JAVA17);
        CreateFunctionAppDialog createDialog = new CreateFunctionAppDialog(shell, defaultConfig);
        createDialog.setOkActionListener(config -> {
            createDialog.close();
            AzureTaskManager.getInstance().runInBackground(
                    AzureString.format("Creating function app %s", config.getName()),
                    () -> new CreateOrUpdateFunctionAppTask(FunctionAppConfig.convertToTaskConfig(config)).execute());
        });
        createDialog.open();
    }
}
